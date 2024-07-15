
package com.cst438.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Grade;
import com.cst438.domain.GradeRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.GradeDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * example of unit test to add a section to an existing course
 */

@AutoConfigureMockMvc
@SpringBootTest
public class AssignmentControllerUnitTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    GradeRepository gradeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void createValidAssignment() throws Exception {
        // Find Section
        Section section = sectionRepository.findById(1).orElse(null);
        String dueDate = "2023-09-01";
        String assignmentTitle = "testAssginment";
        AssignmentDTO assignmentDTO = new AssignmentDTO(
                0,
                assignmentTitle,
                dueDate,
                section.getCourse().getCourseId(),
                section.getSecId(),
                section.getSectionNo());

        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders
                        .post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentDTO)))
                .andReturn().getResponse();

        assertEquals(200, response.getStatus());

        List<Assignment> assignments = assignmentRepository.findBySectionNoOrderByDueDate(1);
        // Assert Assignment added to DB
        assertTrue(assignments.stream().anyMatch(assignment -> assignment.getTitle().equals(assignmentTitle)
                && assignment.getDueDate().toString().equals(dueDate)));

    }

    @Test
    public void createBadDateAssignment() throws Exception {
        // Find Section
        Section section = sectionRepository.findById(1).orElse(null);
        String dueDate = "2023-01-01";
        String assignmentTitle = "testAssginment";
        AssignmentDTO assignmentDTO = new AssignmentDTO(
                0,
                assignmentTitle,
                dueDate,
                section.getCourse().getCourseId(),
                section.getSecId(),
                section.getSectionNo());

        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders
                        .post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentDTO)))
                .andReturn().getResponse();

        assertEquals(400, response.getStatus());
        assertEquals("due date not within course dates", response.getErrorMessage());

    }

    @Test
    public void createBadSecNoAssignment() throws Exception {
        // Find Section
        String dueDate = "2023-09-01";
        String assignmentTitle = "testAssginment";
        AssignmentDTO assignmentDTO = new AssignmentDTO(
                0,
                assignmentTitle,
                dueDate,
                "cst363",
                1,
                -1);

        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders
                        .post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentDTO)))
                .andReturn().getResponse();

        assertEquals(400, response.getStatus());
        assertEquals("section not found", response.getErrorMessage());

    }

    @Test
    public void gradeAssignment() throws Exception {
        // List<Assignment> assignments =
        // assignmentRepository.findBySectionNoOrderByDueDate(8);
        List<Grade> grades = gradeRepository.findByAssignmentId(1);
        int blanketScore = 42;
        List<GradeDTO> gradedAssignments = grades.stream().map(g -> {
            return new GradeDTO(
                    g.getGradeId(),
                    g.getEnrollment().getUser().getName(),
                    g.getEnrollment().getUser().getEmail(),
                    g.getAssignment().getTitle(),
                    g.getEnrollment().getSection().getCourse().getCourseId(),
                    g.getEnrollment().getSection().getSecId(),
                    blanketScore);
        }).toList();
        MockHttpServletResponse response = mvc.perform(
                MockMvcRequestBuilders
                        .put("/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradedAssignments)))
                .andReturn().getResponse();
        List<Grade> gradesUpdate = gradeRepository.findByAssignmentId(1);

        assertEquals(200, response.getStatus());
        assertTrue(gradesUpdate.stream().allMatch(g -> g.getScore().equals(blanketScore)));

    }
    // instructor grades an assignment and enters scores for all enrolled students
    // and uploads the scores

}
