package com.cst438.service;

import com.cst438.controller.EnrollmentController;
import com.cst438.domain.*;
import com.cst438.dto.CourseDTO;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class RegistrarServiceProxy {

    Queue registrarServiceQueue = new Queue("registrar_service", true);
    @Autowired
    private OrderedFormContentFilter formContentFilter;

    @Bean
    public Queue createQueue() {
        return new Queue("gradebook_service", true);
    }

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    CourseRepository courseRepository;
    @Autowired
    SectionRepository sectionRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    EnrollmentRepository enrollmentRepository;
    @Autowired
    TermRepository termRepository;

    // Added code to the file
    @RabbitListener(queues = "gradebook_service")
    public void receiveFromRegister(String message){
        String[] parts = message.split(" ", 2);
        String action = parts[0];
        String data = parts[1];

        System.out.println("Receiving: " + message);

        try{
            switch (action) {
                case "addedCourse" -> addCourse(data);
                case "updatedCourse" -> updateCourse(data);
                case "deletedCourse" -> deleteCourse(data);
                case "addedSection" -> addSection(data);
                case "updatedSection" -> updateSection(data);
                case "deletedSection" -> deleteSection(data);
                case "addedUser" -> addUser(data);
                case "updatedUser" -> updateUser(data);
                case "deletedUser" -> deleteUser(data);
                case "addedEnrollment" -> addEnrollment(data);
                case "deletedEnrollment" -> deleteEnrollment(data);
                default -> throw new IllegalArgumentException("Unknown instruction: " + action);
            }
        }catch (Exception e){
            System.out.println("Error processing action:"+ message + "\nError: " + e);
        }
    }

    public void addCourse(String data) throws Exception {
        CourseDTO dto = fromJsonString(data, CourseDTO.class);
        Course course = new Course();
        course.setCredits(dto.credits());
        course.setTitle(dto.title());
        course.setCourseId(dto.courseId());
        courseRepository.save(course);
    }

    public void updateCourse(String data) throws Exception {
        CourseDTO dto = fromJsonString(data, CourseDTO.class);
        Course course = courseRepository.findById(dto.courseId()).orElseThrow(() -> new Exception("Course not found: " + dto.courseId()));
        course.setCredits(dto.credits());
        course.setTitle(dto.title());
        courseRepository.save(course);
    }

    public void deleteCourse(String data) throws Exception {
        if(courseRepository.existsById(data)) {
            courseRepository.deleteById(data);
        }else{
            throw new Exception("Course not found: " + data);
        }
    }

    public void addSection(String data) throws Exception {
        SectionDTO sectionDTO = fromJsonString(data, SectionDTO.class);
        Course course = courseRepository.findById(sectionDTO.courseId()).orElseThrow(() -> new Exception("Course not found: " + sectionDTO.courseId()));
        Term term = termRepository.findByYearAndSemester(sectionDTO.year(), sectionDTO.semester());

        if(term == null) {
            throw new Exception("Year, semester invalid: " + sectionDTO.year() + ", " + sectionDTO.semester());
        }

        Section section = new Section();
        section.setCourse(course);
        section.setSectionNo(sectionDTO.secNo());
        section.setTerm(term);
        section.setSecId(sectionDTO.secId());
        section.setBuilding(sectionDTO.building());
        section.setRoom(sectionDTO.room());
        section.setTimes(sectionDTO.times());

        if(sectionDTO.instructorEmail() != null && !sectionDTO.instructorEmail().isEmpty()) {
            User instructor = userRepository.findByEmail(sectionDTO.instructorEmail());
            if(instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
                throw new Exception("Email not found or invalid instructor: " + sectionDTO.instructorEmail());
            }
            section.setInstructor_email(sectionDTO.instructorEmail());
        }
        sectionRepository.save(section);
    }

    public void updateSection(String data) throws Exception {
        SectionDTO sectionDTO = fromJsonString(data, SectionDTO.class);
        Section section = sectionRepository.findById(sectionDTO.secNo()).orElseThrow(() -> new Exception("Section not found: " + sectionDTO.secNo()));
        section.setSecId(sectionDTO.secId());
        section.setBuilding(sectionDTO.building());
        section.setRoom(sectionDTO.room());
        section.setTimes(sectionDTO.times());

        if(sectionDTO.instructorEmail() != null && !sectionDTO.instructorEmail().isEmpty()) {
            User instructor = userRepository.findByEmail(sectionDTO.instructorEmail());
            if(instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
                throw new Exception("Email not found or invalid instructor: " + sectionDTO.instructorEmail());
            }
            section.setInstructor_email(sectionDTO.instructorEmail());
        }
        sectionRepository.save(section);
    }

    public void deleteSection(String data) throws Exception {
        try {
            Integer id = Integer.parseInt(data);
            sectionRepository.deleteById(id);
        } catch (NumberFormatException e) {
            throw new Exception("Section not found: " + data);
        }
    }

    public void addUser(String data) throws Exception {
        UserDTO dto = fromJsonString(data, UserDTO.class);
        User user = new User();
        user.setId(dto.id());
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword("");       // <- empty password?
        user.setType(dto.type());

        if(!ValidUser(dto.type())){
            throw new Exception("Invalid user: " + dto.type());
        }
        userRepository.save(user);
    }

    public void updateUser(String data) throws Exception{
        UserDTO dto = fromJsonString(data, UserDTO.class);
        User user = userRepository.findById(dto.id()).orElseThrow(() -> new Exception("User not found: " + dto.id()));
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setType(dto.type());

        if(!ValidUser(dto.type())){
            throw new Exception("Invalid user: " + dto.type());
        }
        userRepository.save(user);
    }

    public void deleteUser(String data) throws Exception {
        try {
            Integer id = Integer.parseInt(data);
            userRepository.deleteById(id);
        } catch (NumberFormatException e){
            throw new Exception("Invalid user: " + data);
        }
    }

    public void addEnrollment(String data) throws Exception{
        EnrollmentDTO dto = fromJsonString(data, EnrollmentDTO.class);
        User student = userRepository.findById(dto.studentId()).orElseThrow(() -> new Exception("Student not found: " + dto.studentId()));
        Section section = sectionRepository.findById(dto.sectionNo()).orElseThrow(() -> new Exception("Section not found: " + dto.sectionNo()));
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(dto.enrollmentId());
        enrollment.setGrade(dto.grade());
        enrollment.setUser(student);
        enrollment.setSection(section);
        enrollmentRepository.save(enrollment);
    }

    public void deleteEnrollment(String data) throws Exception{
        try {
            Integer id = Integer.parseInt(data);
            enrollmentRepository.deleteById(id);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid enrollment ID: " + data);
        }
    }

    public void updateEnrollment(EnrollmentDTO eDTO) {
        String msg = "updatedEnrollment " + asJsonString(eDTO);
        sendMessage(msg);
    }

    //check if it's a correct user
    private boolean ValidUser(String user){
        return user.equals("STUDENT") || user.equals("INSTRUCTOR") || user.equals("ADMIN");
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleUnkownInstruction(String instruction) {
        throw new IllegalArgumentException("Unknown instruction: " + instruction);
    }

    private void sendMessage(String s) {
        System.out.println("Sending: " + s);
        rabbitTemplate.convertAndSend(registrarServiceQueue.getName(), s);
    }

    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T  fromJsonString(String str, Class<T> valueType ) {
        try {
            return new ObjectMapper().readValue(str, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}