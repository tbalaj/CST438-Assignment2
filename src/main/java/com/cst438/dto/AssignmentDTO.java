package com.cst438.dto;
import java.sql.Date;


/*
 * Data Transfer Object for assignment data
 */
public record AssignmentDTO(
        int assignmentId,
        String title,
        Date dueDate,
        String courseId,
        int secId,
        int secNo

) {
}
