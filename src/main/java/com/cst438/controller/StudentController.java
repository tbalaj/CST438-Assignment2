package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.GradeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.function.ToDoubleBiFunction;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class StudentController {

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    TermRepository termRepository;

    @Autowired
    GradeRepository gradeRepository;

    @Autowired
    AssignmentRepository assignmentRepository;



   // student gets transcript showing list of all enrollments
   // studentId will be temporary until Login security is implemented
   //example URL  /transcript?studentId=19803
   @GetMapping("/transcripts")
   public List<EnrollmentDTO> getTranscript(@RequestParam("studentId") int studentId) {

       // TODO

       // list course_id, sec_id, title, credit, grade in chronological order
       // user must be a student
	   // hint: use enrollment repository method findEnrollmentByStudentIdOrderByTermId
       // remove the following line when done
       List<EnrollmentDTO> transcript = new ArrayList<EnrollmentDTO>();
       List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(studentId);
       for(Enrollment enrollment : enrollments) {
           if(!enrollment.getStudent().getType().equals("STUDENT")){
               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user is not a student");
           } else if (studentId != enrollment.getStudent().getId()){
               throw new ReponseStatusException(HttpStatus.BAD_REQUEST, "user is not a student id does not match");
           }
           transcript.add(
                   new EnrollmentDTO(
                           enrollment.getEnrollmentId(),
                           enrollment.getGrade(),
                           enrollment.getStudent().getId(),
                           enrollment.getStudent().getName(),
                           enrollment.getStudent().getEmail(),
                           enrollment.getSection().getCourse().getCourseId(),
                           enrollment.getSection().getCourse.getTitle(),
                           enrollment.getSection().getSecId(),
                           enrollment.getSection().getSectionNo(),
                           enrollment.getSection().getBuilding(),
                           enrollment.getSection().getRoom(),
                           enrollment.getSection().getTimes(),
                           enrollment.getSection().getCourse().getCredits(),
                           enrollment.getSection().getTerm().getYear(),
                           enrollment.getSection().getTerm().getSemester()
                   )
           );
       }
       return transcript;
   }

    // student gets a list of their enrollments for the given year, semester
    // user must be student
    // studentId will be temporary until Login security is implemented
   @GetMapping("/enrollments")
   public List<EnrollmentDTO> getSchedule(
           @RequestParam("year") int year,
           @RequestParam("semester") String semester,
           @RequestParam("studentId") int studentId) {

       List<EnrollmentDTO> schedule = new ArrayList<>();
       List<Enrollment> enrollments = enrollmentRepository.findByYearAndSemesterOrderByCourseId(year, semester, studentId);

       for(Enrollment enrollment : enrollments) {
           if(!enrollment.getStudent.getType().equals("STUDENT")){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user is not a student");
           } else if (studentId != enrollment.getStudent().getId()) {
               throw new ReponseStatusException(HttpStatus.BAD_REQUEST, "student id does not match");
           }
           schedule.add(
                   new EnrollmentDTO(
                           enrollment.getEnrollmentId(),
                           enrollment.getGrade(),
                           enrollment.getStudent().getId(),
                           enrollment.getStudent().getName(),
                           enrollment.getStudent().getEmail(),
                           enrollment.getSection().getCourse().getCourseId(),
                           enrollment.getSection().getCourse().getTitle(),
                           enrollment.getSection().getSecId(),
                           enrollment.getSection().getSectionNo(),
                           enrollment.getSection().getBuilding(),
                           enrollment.getSection().getRoom(),
                           enrollment.getSection().getTimes(),
                           enrollment.getSection().getCourse().getCredits(),
                           enrollment.getSection().getTerm().getYear(),
                           enrollment.getSection().getTerm().getSenester()
                   )
           );
       }

     // TODO
	 //  hint: use enrollment repository method findByYearAndSemesterOrderByCourseId
     //  remove the following line when done
       return schedule;
   }


    // student adds enrollment into a section
    // user must be student
    // return EnrollmentDTO with enrollmentId generated by database
    @PostMapping("/enrollments/sections/{sectionNo}")
    public EnrollmentDTO addCourse(
		    @PathVariable int sectionNo,
            @RequestParam("studentId") int studentId ) {

        // TODO

        // check that the Section entity with primary key sectionNo exists
        // check that today is between addDate and addDeadline for the section
        // check that student is not already enrolled into this section
        // create a new enrollment entity and save.  The enrollment grade will
        // be NULL until instructor enters final grades for the course.

        User user = userRepository.findById(studentId).orElse(null);
        if(user == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user not found");
        }

        Section section = sectionRepository.findById(sectionNo).orElse(null);
        if(section == null){
            throw new ResponseStatusException(HtppStatus.BAD_REQUEST, "section not valid");
        }

        Term term = termRepository.findById(section.getTerm().getTermId()).orElse(null);
        if(term == null){
            throw new ReponseStatusException(HttpStatus.BAD_REQUEST, "term doesn't exist");
        }

        //LocalDate addDate = term.getAddDate().toLocalDate();
        Date addDate = term.getAddDate();
        Date addDeadLine = term.getAddDeadline();
        if(new Date().before(addDate)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't add prior to add date");
        }
        if(new Date().after(addDeadLine)){
            throw new ReponseStatusException(HttpStatus.BAD_REQUEST, "You missed the dead line!");
        }

        Enrollment enrollExist = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(sectionNo, studentId);
        if(enrollExist != null){
            throw new ReponseStatusException(HttpStatus.BAD_REQUEST, "Already enrolled");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setGrade(null);
        enrollment.setStudent(user);
        enrollment.setSection(section);

        // remove the following line when done.
        return new EnrollmentDTO(
                enrollment.getEnrollmentId(),
                enrollment.getGrade(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getName(),
                enrollment.getStudent().getEmail(),
                enrollment.getSection().getCourse().getCourseId(),
                enrollment.getSection().getCourse().getTitle(),
                enrollment.getSection().SecId(),
                enrollment.getSection().getSectionNo(),
                enrollment.getSection().getBuilding(),
                enrollment.getSection().getRoom(),
                enrollment.getSection().getTimes(),
                enrollment.getSection().getCourse().getCredits(),
                enrollment.getSection().getTerm().getYear(),
                enrollment.getSection().getTerm().getSemester()

        );

    }

    // student drops a course
    // user must be student
   @DeleteMapping("/enrollments/{enrollmentId}")
   public void dropCourse(@PathVariable("enrollmentId") int enrollmentId) {

       // TODO
       // check that today is not after the dropDeadline for section
   }
}