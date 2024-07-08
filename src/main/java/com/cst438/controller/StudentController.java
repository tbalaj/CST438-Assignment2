package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
           if(!enrollment.getUser().getType().equals("STUDENT")){
               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user is not a student");
           } else if (studentId != enrollment.getUser().getId()){
               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user is not a student id does not match");
           }
           transcript.add(
                   new EnrollmentDTO(
                           enrollment.getEnrollmentId(),
                           enrollment.getGrade(),
                           enrollment.getUser().getId(),
                           enrollment.getUser().getName(),
                           enrollment.getUser().getEmail(),
                           enrollment.getSection().getCourse().getCourseId(),
                           enrollment.getSection().getCourse().getTitle(),
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
           if(!enrollment.getUser().getType().equals("STUDENT")){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user is not a student");
           } else if (studentId != enrollment.getUser().getId()) {
               throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "student id does not match");
           }
           schedule.add(
                   new EnrollmentDTO(
                           enrollment.getEnrollmentId(),
                           enrollment.getGrade(),
                           enrollment.getUser().getId(),
                           enrollment.getUser().getName(),
                           enrollment.getUser().getEmail(),
                           enrollment.getSection().getCourse().getCourseId(),
                           enrollment.getSection().getCourse().getTitle(),
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "section not valid");
        }

        Term term = termRepository.findById(section.getTerm().getTermId()).orElse(null);
        if(term == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "term doesn't exist");
        }

        Date addDate = term.getAddDate();
        Date addDeadLine = term.getAddDeadline();
        if(new Date().before(addDate)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't add prior to add date");
        }
        if(new Date().after(addDeadLine)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You missed the dead line!");
        }

        Enrollment enrollExist = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(sectionNo, studentId);
        if(enrollExist != null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already enrolled");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setGrade(null);
        enrollment.setUser(user);
        enrollment.setSection(section);
        enrollmentRepository.save(enrollment);

        // remove the following line when done.
        return new EnrollmentDTO(
                enrollment.getEnrollmentId(),
                enrollment.getGrade(),
                enrollment.getUser().getId(),
                enrollment.getUser().getName(),
                enrollment.getUser().getEmail(),
                enrollment.getSection().getCourse().getCourseId(),
                enrollment.getSection().getCourse().getTitle(),
                enrollment.getSection().getSecId(),
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
       Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElse(null);
       if(enrollment == null){
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid enrollment id.");
       }

       Section section = sectionRepository.findById(enrollment.getSection().getSectionNo()).orElse(null);
       if (section == null) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid section number.");
       }

       Term term = termRepository.findById(section.getTerm().getTermId()).orElse(null);
       if (term == null) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid term.");
       }

       User user = userRepository.findById(enrollment.getUser().getId()).orElse(null);
       if(user == null){
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid user id.");
       }

       Grade grade = gradeRepository.findByEnrollmentId(enrollment.getEnrollmentId());
       if (grade != null) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade has already been given.");
       }

       Date dropDeadline = term.getDropDeadline();
       if (new Date().after(dropDeadline)) {
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passed the drop deadline date.");
       }

       enrollmentRepository.delete(enrollment);
   }
}