package com.cst438.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Course;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.Term;
import com.cst438.domain.TermRepository;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.service.GradebookServiceProxy;

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
    GradebookServiceProxy gradeBookService;

    // @Autowired
    // GradeRepository gradeRepository;

    // @Autowired
    // AssignmentRepository assignmentRepository;

    // student gets transcript showing list of all enrollments
    // studentId will be temporary until Login security is implemented
    // example URL /transcript?studentId=19803
    @GetMapping("/transcripts")
    public List<EnrollmentDTO> getTranscript(@RequestParam("studentId") int studentId) {

        // TODO

        // list course_id, sec_id, title, credit, grade in chronological order
        // user must be a student
        // hint: use enrollment repository method findEnrollmentByStudentIdOrderByTermId
        // remove the following line when done
        List<EnrollmentDTO> transcript = new ArrayList<EnrollmentDTO>();
        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(studentId);
        for (Enrollment enrollment : enrollments) {
            if (!enrollment.getUser().getType().equals("STUDENT")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user is not a student");
            } else if (studentId != enrollment.getUser().getId()) {
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
                            enrollment.getSection().getTerm().getSemester()));
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
        List<Enrollment> enrollments = enrollmentRepository.findByYearAndSemesterOrderByCourseId(year, semester,
                studentId);

        for (Enrollment enrollment : enrollments) {
            if (!enrollment.getUser().getType().equals("STUDENT")) {
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
                            enrollment.getSection().getTerm().getSemester()));
        }

        // TODO
        // hint: use enrollment repository method findByYearAndSemesterOrderByCourseId
        // remove the following line when done
        return schedule;
    }

    // student adds enrollment into a section
    // user must be student
    // return EnrollmentDTO with enrollmentId generated by database
    @PostMapping("/enrollments/sections/{sectionNo}")
    public EnrollmentDTO addCourse(
            @PathVariable int sectionNo,
            @RequestParam("studentId") int studentId) {

        User user = userRepository.findById(studentId).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
        }

        Section section = sectionRepository.findById(sectionNo).orElse(null);
        if (section == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section not valid");
        }

        Term term = section.getTerm();
        if (term == null || !termRepository.existsById(term.getTermId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Term doesn't exist");
        }

        Date currentDate = new Date();
        if (currentDate.before(term.getAddDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't add prior to add date");
        }
        if (currentDate.after(term.getAddDeadline())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You missed the deadline!");
        }

        Enrollment existingEnrollment = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(sectionNo,
                studentId);
        if (existingEnrollment != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already enrolled");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setGrade(null);
        enrollment.setUser(user);
        enrollment.setSection(section);
        enrollmentRepository.save(enrollment);

        Course course = section.getCourse();
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Course is not assigned to this section");
        }

        EnrollmentDTO newEnrolment = new EnrollmentDTO(
                enrollment.getEnrollmentId(),
                enrollment.getGrade(),
                enrollment.getUser().getId(),
                enrollment.getUser().getName(),
                enrollment.getUser().getEmail(),
                course.getCourseId(),
                course.getTitle(),
                section.getSecId(),
                section.getSectionNo(),
                section.getBuilding(),
                section.getRoom(),
                section.getTimes(),
                course.getCredits(),
                term.getYear(),
                term.getSemester());

        gradeBookService.addEnrollment(newEnrolment);
        return newEnrolment;
    }

    // student drops a course
    // user must be student
    @DeleteMapping("/enrollments/{enrollmentId}")
    public void dropCourse(@PathVariable("enrollmentId") int enrollmentId) {

        // TODO
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElse(null);
        if (enrollment == null) {
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
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid user id.");
        }
        // TODO Omitted for assignment 6, fix?
        // Grade grade =
        // gradeRepository.findByEnrollmentId(enrollment.getEnrollmentId());
        // if (grade != null) {
        // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grade has already
        // been given.");
        // }

        Date dropDeadline = term.getDropDeadline();
        if (new Date().after(dropDeadline)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passed the drop deadline date.");
        }

        EnrollmentDTO newEnrolment = new EnrollmentDTO(
                enrollment.getEnrollmentId(),
                enrollment.getGrade(),
                enrollment.getUser().getId(),
                enrollment.getUser().getName(),
                enrollment.getUser().getEmail(),
                enrollment.getSection().getCourse().getCourseId(),
                enrollment.getSection().getCourse().getTitle(),
                section.getSecId(),
                section.getSectionNo(),
                section.getBuilding(),
                section.getRoom(),
                section.getTimes(),
                enrollment.getSection().getCourse().getCredits(),
                term.getYear(),
                term.getSemester());

        gradeBookService.deleteEnrollment(newEnrolment);
        enrollmentRepository.delete(enrollment);
    }
}