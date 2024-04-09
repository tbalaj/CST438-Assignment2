package com.cst438.service;
import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.GradeDTO;
import com.cst438.dto.CourseDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Optional;

@Service
public class RegistrarServiceProxy {

    Queue registrarServiceQueue = new Queue("registrar_service", true);

    @Bean
    public Queue createQueue() {
        return new Queue("gradebook_service", true);
    }

    @Autowired
    RabbitTemplate rabbitTemplate;
    CourseRepository courseRepository;
    TermRepository termRepository;
    UserRepository userRepository;
    SectionRepository sectionRepository;
    EnrollmentRepository enrollmentRepository;

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @RabbitListener(queues = "gradebook_service")
    public void receiveFromRegistrar(String message)  {
        try {
            System.out.println("Received from Registrar: " + message);
            String[] parts = message.split(" ", 2);
            switch (parts[0]) {
                case "addCourse":
                    CourseDTO courseAddDTO = fromJsonString(parts[1], CourseDTO.class);
                    Course courseAdd = new Course();
                    courseAdd.setCredits(courseAddDTO.credits());
                    courseAdd.setTitle(courseAddDTO.title());
                    courseAdd.setCourseId(courseAddDTO.courseId());
                    courseRepository.save(courseAdd);
                    break;

                case "updateCourse":
                    CourseDTO courseUpdateDTO = fromJsonString(parts[1], CourseDTO.class);
                    Course courseUpdate = courseRepository.findById(courseUpdateDTO.courseId()).orElse(null);
                    if (courseUpdate == null) {
                        throw new ResponseStatusException( HttpStatus.NOT_FOUND, "course not found: "+courseUpdateDTO.courseId());
                    } else {
                        courseUpdate.setTitle(courseUpdateDTO.title());
                        courseUpdate.setCredits(courseUpdateDTO.credits());
                        courseRepository.save(courseUpdate);
                    }
                    break;

                case "deleteCourse":
                    String courseId = parts[1];
                    courseRepository.findById(courseId).ifPresent(
                            courseToDelete -> courseRepository.delete(courseToDelete));
                    break;

                case "addSection":
                    SectionDTO sectionAddDTO = fromJsonString(parts[1], SectionDTO.class);

                    // Ensure course exists for which section is attempting to be added
                    Course course = courseRepository.findById(sectionAddDTO.courseId()).orElse(null);
                    if (course == null ){
                        throw  new ResponseStatusException( HttpStatus.BAD_REQUEST, "course not found "+sectionAddDTO.courseId());
                    }
                    Section sectionAdd = new Section();
                    sectionAdd.setCourse(course);

                    // Ensure term exists for which section is attempting to be added
                    Term term = termRepository.findByYearAndSemester(sectionAddDTO.year(), sectionAddDTO.semester());
                    if (term == null) {
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "year, semester invalid ");
                    }
                    sectionAdd.setTerm(term);

                    // Continue padding out section entity
                    sectionAdd.setSecId(sectionAddDTO.secId());
                    sectionAdd.setBuilding(sectionAddDTO.building());
                    sectionAdd.setRoom(sectionAddDTO.room());
                    sectionAdd.setTimes(sectionAddDTO.times());

                    // Set the instructor to be a known user if the email correlates with what's in the repository
                    User instructor = null;
                    if (sectionAddDTO.instructorEmail()==null || sectionAddDTO.instructorEmail().isEmpty()) {
                        sectionAdd.setInstructor_email("");
                    } else {
                        instructor = userRepository.findByEmail(sectionAddDTO.instructorEmail());
                        if (instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "email not found or not an instructor " + sectionAddDTO.instructorEmail());
                        }
                        sectionAdd.setInstructor_email(sectionAddDTO.instructorEmail());
                    }

                    sectionRepository.save(sectionAdd);
                    break;

                case "updateSection":
                    SectionDTO sectionUpdateDTO = fromJsonString(parts[1], SectionDTO.class);

                    // Ensure section exists which we are trying to modify
                    Section sectionUpdate = sectionRepository.findById(sectionUpdateDTO.secNo()).orElse(null);
                    if (sectionUpdate==null) {
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND,
                                "section not found "+sectionUpdateDTO.secNo());
                    }

                    sectionUpdate.setSecId(sectionUpdateDTO.secId());
                    sectionUpdate.setBuilding(sectionUpdateDTO.building());
                    sectionUpdate.setRoom(sectionUpdateDTO.room());
                    sectionUpdate.setTimes(sectionUpdateDTO.times());

                    User instructorUpdate = null;
                    if (sectionUpdateDTO.instructorEmail()==null || sectionUpdateDTO.instructorEmail().isEmpty()) {
                        sectionUpdate.setInstructor_email("");
                    } else {
                        instructorUpdate = userRepository.findByEmail(sectionUpdateDTO.instructorEmail());
                        if (instructorUpdate == null || !instructorUpdate.getType().equals("INSTRUCTOR")) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "email not found or not an instructor " + sectionUpdateDTO.instructorEmail());
                        }
                        sectionUpdate.setInstructor_email(sectionUpdateDTO.instructorEmail());
                    }

                    sectionRepository.save(sectionUpdate);
                    break;

                case "deleteSection":
                    int sectionNo = Integer.parseInt(parts[1]);
                    sectionRepository.findById(sectionNo).ifPresent(s -> sectionRepository.delete(s));
                    break;

                case "addUser":
                    UserDTO userAddDTO = fromJsonString(parts[1], UserDTO.class);
                    User userAdd = new User();
                    userAdd.setName(userAddDTO.name());
                    userAdd.setEmail(userAddDTO.email());

                    // create password and encrypt it
                    String password = userAddDTO.name()+"2024";
                    String enc_password = encoder.encode(password);
                    userAdd.setPassword(enc_password);

                    userAdd.setType(userAddDTO.type());
                    if (!userAddDTO.type().equals("STUDENT") &&
                            !userAddDTO.type().equals("INSTRUCTOR") &&
                            !userAddDTO.type().equals("ADMIN")) {
                        // invalid type
                        throw  new ResponseStatusException( HttpStatus.BAD_REQUEST, "invalid user type");
                    }
                    userRepository.save(userAdd);
                    break;

                case "updateUser":
                    UserDTO userUpdateDTO = fromJsonString(parts[1], UserDTO.class);
                    User userUpdate = userRepository.findById(userUpdateDTO.id()).orElse(null);
                    if (userUpdate==null) {
                        throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "user id not found");
                    }
                    userUpdate.setName(userUpdateDTO.name());
                    userUpdate.setEmail(userUpdateDTO.email());
                    userUpdate.setType(userUpdateDTO.type());
                    if (!userUpdateDTO.type().equals("STUDENT") &&
                            !userUpdateDTO.type().equals("INSTRUCTOR") &&
                            !userUpdateDTO.type().equals("ADMIN")) {
                        // invalid type
                        throw  new ResponseStatusException( HttpStatus.BAD_REQUEST, "invalid user type");
                    }
                    userRepository.save(userUpdate);

                    break;

                case "deleteUser":
                    int userId = Integer.parseInt(parts[1]);
                    userRepository.findById(userId).ifPresent(u -> userRepository.delete(u));
                    break;

                case "addEnrollment":
                    EnrollmentDTO enrollmentAddDTO = fromJsonString(parts[1], EnrollmentDTO.class);

                    Enrollment e = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(
                            enrollmentAddDTO.sectionNo(), enrollmentAddDTO.studentId());

                    if(e != null){
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student already enrolled");
                    }

                    e = new Enrollment();

                    User student = userRepository.findById(enrollmentAddDTO.studentId()).orElse(null);
                    if (student==null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "student id not found");
                    }

                    e.setUser(student);
                    Section section = sectionRepository.findById(enrollmentAddDTO.sectionNo()).orElse(null);
                    if(section == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "section number not found");
                    }

                    Date todayAdd = new Date();
                    if (todayAdd.before(section.getTerm().getAddDate()) || todayAdd.after(section.getTerm().getAddDeadline())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not within the enrollment period");
                    }

                    e.setSection(section);
                    e.setGrade(null);
                    enrollmentRepository.save(e);

                    break;

                case "deleteEnrollment":
                    int enrollmentId = Integer.parseInt(parts[1]);
                    Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(enrollmentId);

                    if (enrollmentOpt.isEmpty()) {
                        throw new ResponseStatusException( HttpStatus.NOT_FOUND,
                                "enrollment information not found with ID "+enrollmentId);
                    }

                    Enrollment enrollment = enrollmentOpt.get();

                    Date dropDeadline = enrollment.getSection().getTerm().getDropDeadline();
                    Date todayDelete = new Date();

                    if (todayDelete.after(dropDeadline)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cannot drop course after the drop deadline");
                    }

                    enrollmentRepository.deleteById(enrollmentId);
                    break;

                default:
                    System.out.println("Unknown command received: " + parts[0]);
            }
        } catch (Exception e) {
            System.out.println("Exception in receiveFromRegistrar " + e.getMessage());
        }
    }


    private void sendMessage(String s) {
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

    public void createAssignment(AssignmentDTO a) { sendMessage("createAssignment " + asJsonString(a)); }

    public void updateAssignment(AssignmentDTO a) { sendMessage("updateAssignment " + asJsonString(a)); }

    public void deleteAssignment(int id) { sendMessage("deleteAssignment " + id); }

    public void updateGrade(GradeDTO g) { sendMessage("updateGrade " + asJsonString(g)); }

    public void updateEnrollmentGrade(EnrollmentDTO e) { sendMessage("updateEnrollmentGrade " + asJsonString(e)); }




}