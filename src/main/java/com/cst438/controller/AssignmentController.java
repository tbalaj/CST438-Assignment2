package com.cst438.controller;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.cst438.domain.*;
import com.cst438.dto.SectionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.GradeDTO;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class AssignmentController {

    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    UserRepository userRepository;


    // instructor lists assignments for a section. Assignments ordered by due date.
    // logged in user must be the instructor for the section
    @GetMapping("/sections/{secNo}/assignments")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo) {
        try {
            // hint: use the assignment repository method
            // findBySectionNoOrderByDueDate to return
            // a list of assignments
            List<Assignment> assignments = assignmentRepository.findBySectionNoOrderByDueDate(secNo);
            return assignments.stream()
                    .map(assignment -> {
                        return new AssignmentDTO(
                                assignment.getAssignmentId(),
                                assignment.getTitle(),
                                assignment.getDueDate().toString(),
                                assignment.getSection().getCourse().getCourseId(),
                                assignment.getSection().getSecId(),
                                assignment.getSection().getSectionNo());

                    }).toList();
        } catch (Exception e) {
            return null;
        }
    }

    // add assignment
    // user must be instructor of the section
    // return AssignmentDTO with assignmentID generated by database
    @PostMapping("/assignments")
    public AssignmentDTO createAssignment(
            @RequestBody AssignmentDTO dto) {
        Assignment newAssignment = new Assignment();
        Section newSection = sectionRepository.findById(dto.secNo()).orElse(null);
        if (newSection == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "section not found");
        } 
        Date dueDate = Date.valueOf(dto.dueDate());
        Date starDate = newSection.getTerm().getStartDate();
        Date endDate = newSection.getTerm().getEndDate();
        
        if(dueDate.after(endDate) || dueDate.before(starDate)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "due date not within course dates");
        }

        newAssignment.setDue_Date(java.sql.Date.valueOf(dto.dueDate()));
        newAssignment.setSection(newSection);
        newAssignment.setTitle(dto.title());
        Assignment response = assignmentRepository.save(newAssignment);

        return new AssignmentDTO(
                response.getAssignmentId(),
                response.getTitle(),
                response.getDueDate().toString(),
                response.getSection().getCourse().getCourseId(),
                response.getSection().getSecId(),
                response.getSection().getSectionNo());
    }

    // update assignment for a section. Only title and dueDate may be changed.
    // user must be instructor of the section??
    // return updated AssignmentDTO
    @PutMapping("/assignments")
    public AssignmentDTO updateAssignment(@RequestBody AssignmentDTO dto) {
        Assignment assignment = assignmentRepository.findById(dto.id()).orElse(null);
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "assignment not found ");
        }
        assignment.setTitle(dto.title());
        Date dueDate = java.sql.Date.valueOf(dto.dueDate());
        assignment.setDue_Date(dueDate);
        Assignment updatedAssignment = assignmentRepository.save(assignment);
        return new AssignmentDTO(
                updatedAssignment.getAssignmentId(),
                updatedAssignment.getTitle(),
                updatedAssignment.getDueDate().toString(),
                updatedAssignment.getSection().getCourse().getCourseId(),
                updatedAssignment.getSection().getSecId(),
                updatedAssignment.getSection().getSectionNo());
    }

    // delete assignment for a section
    // logged in user must be instructor of the section
    @DeleteMapping("/assignments/{assignmentId}")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment != null) {
            // Find all grades associated with this assignment
            List<Grade> grades = gradeRepository.findByAssignmentId(assignmentId);

            // Delete all grades associated with the assignment
            gradeRepository.deleteAll(grades);

            // Delete the assignment
            assignmentRepository.delete(assignment);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }
    }

    // instructor gets grades for assignment ordered by student name
    // user must be instructor for the section
    @GetMapping("/assignments/{assignmentId}/grades")
    public List<GradeDTO> getAssignmentGrades(@PathVariable("assignmentId") int assignmentId) {

        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }
        // get the list of enrollments for the section related to this assignment.
        // hint: use te enrollment repository method
        // findEnrollmentsBySectionOrderByStudentName.
        List<Enrollment> enrollments = enrollmentRepository
                .findEnrollmentsBySectionNoOrderByStudentName(assignment.getSection().getSectionNo());
        // for each enrollment, get the grade related to the assignment and enrollment
        return enrollments.stream()
                .map(enrollment -> {
                    // hint: use the gradeRepository findByEnrollmentIdAndAssignmentId method.
                    Grade g = gradeRepository.findByEnrollmentIdAndAssignmentId(enrollment.getEnrollmentId(),
                            assignment.getAssignmentId());
                    if (g == null) {
                        // if the grade does not exist, create a grade entity and set the score to NULL
                        Grade newGrade = new Grade();
                        newGrade.setScore(null);
                        newGrade.setAssignment(assignment);
                        newGrade.setEnrollment(enrollment);
                        // and then save the new entity
                        g = gradeRepository.save(newGrade);

                    }
                    GradeDTO dto = new GradeDTO(
                            g.getGradeId(),
                            enrollment.getUser().getName(),
                            enrollment.getUser().getEmail(),
                            assignment.getTitle(),
                            enrollment.getSection().getCourse().getCourseId(),
                            enrollment.getSection().getSecId(),
                            g.getScore());
                    return dto;

                }).toList();

    }

    // instructor uploads grades for assignment
    // user must be instructor for the section
    @PutMapping("/grades")
    public ResponseEntity<String> updateGrades(@RequestBody List<GradeDTO> dlist) {
        // List to collect invalid grade IDs
        List<Integer> invalidGradeIds = dlist.stream()
                .filter(grade -> !gradeRepository.existsById(grade.gradeId()))
                .map(GradeDTO::gradeId)
                .collect(Collectors.toList());

        if (!invalidGradeIds.isEmpty()) {
            // Return 400 Bad Request if any grade ID is invalid
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid grade IDs: " + invalidGradeIds);
        }

        // Update valid grades
        dlist.forEach(gradeDTO -> {
            Grade grade = gradeRepository.findById(gradeDTO.gradeId()).orElse(null);
            if (grade != null) {
                grade.setScore(gradeDTO.score());
                gradeRepository.save(grade);
            }
        });

        // Return 200 OK if all grades were updated successfully
        return ResponseEntity.ok("Grades updated successfully");
    }

    // student lists their assignments/grades for an enrollment ordered by due date
    // student must be enrolled in the section
    @GetMapping("/assignments")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("studentId") int studentId,
            @RequestParam("year") int year,
            @RequestParam("semester") String semester) {

        // return a list of assignments and (if they exist) the assignment grade
        // for all sections that the student is enrolled for the given year and semester
        // hint: use the assignment repository method
        // findByStudentIdAndYearAndSemesterOrderByDueDate
        List<Assignment> assignments = assignmentRepository.findByStudentIdAndYearAndSemesterOrderByDueDate(studentId,
                year, semester);
        List<AssignmentStudentDTO> dto = assignments
                .stream()
                .map(assignment -> {
                    Enrollment e = enrollmentRepository
                            .findEnrollmentBySectionNoAndStudentId(assignment.getSection().getSectionNo(), studentId);
                    Grade g = gradeRepository.findByEnrollmentIdAndAssignmentId(e.getEnrollmentId(),
                            assignment.getAssignmentId());
                    return new AssignmentStudentDTO(
                            assignment.getAssignmentId(),
                            assignment.getTitle(),
                            assignment.getDueDate(),
                            assignment.getSection().getCourse().getTitle(),
                            assignment.getSection().getSecId(),
                            g != null ? g.getScore() : null);
                }).toList();
        return dto;
    }

    @GetMapping("/sections")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("email") String instructorEmail,
            @RequestParam("year") int year ,
            @RequestParam("semester") String semester )  {


        List<Section> sections = sectionRepository.findByInstructorEmailAndYearAndSemester(instructorEmail, year, semester);

        List<SectionDTO> dto_list = new ArrayList<>();
        for (Section s : sections) {
            User instructor = null;
            if (s.getInstructorEmail()!=null) {
                instructor = userRepository.findByEmail(s.getInstructorEmail());
            }
            dto_list.add(new SectionDTO(
                    s.getSectionNo(),
                    s.getTerm().getYear(),
                    s.getTerm().getSemester(),
                    s.getCourse().getCourseId(),
                    s.getCourse().getTitle(),
                    s.getSecId(),
                    s.getBuilding(),
                    s.getRoom(),
                    s.getTimes(),
                    (instructor!=null) ? instructor.getName() : "",
                    (instructor!=null) ? instructor.getEmail() : ""
            ));
        }
        return dto_list;
    }
}
