package com.cst438.service;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import com.cst438.dto.CourseDTO;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@Service
public class GradebookServiceProxy {
    Queue gradebookServiceQueue = new Queue("gradebook_service", true);

    @Bean
    public Queue createQueue() {
        return new Queue("registrar_service", true);
    }

    @Autowired
    RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "registrar_service")
    public void receiveFromGradebook(String message) {
        // TODO implement this message
    }

    public void addCourse(CourseDTO c) {
        sendMessage("addedCourse " + asJsonString(c));
    }

    public void deleteCourse(CourseDTO c) {
        sendMessage("deletedCourse " + asJsonString(c));
    }

    public void updateCourse(CourseDTO c) {
        sendMessage("updatedCourse " + asJsonString(c));
    }

    public void addSection(SectionDTO s) {
        sendMessage("addedSection " + asJsonString(s));
    }

    public void deleteSection(SectionDTO s) {
        sendMessage("deletedSection " + asJsonString(s));
    }

    public void updateSection(SectionDTO s) {
        sendMessage("updatedSection " + asJsonString(s));
    }

    public void addUser(UserDTO u) {
        sendMessage("addedUser " + asJsonString(u));
    }

    public void deleteUser(UserDTO u) {
        sendMessage("deletedUser " + asJsonString(u));
    }

    public void updateUser(UserDTO u) {
        sendMessage("updatedUser " + asJsonString(u));
    }

    public void addEnrollment(EnrollmentDTO e) {
        sendMessage("addedEnrollment " + asJsonString(e));
    }

    public void deleteEnrollment(EnrollmentDTO e) {
        sendMessage("deletedEnrollment " + asJsonString(e));
    }

    public void updateEnrollment(EnrollmentDTO e) {
        sendMessage("updatedEnrollment " + asJsonString(e));
    }

    private void sendMessage(String s) {
        System.out.println("Sending: " + s);
        rabbitTemplate.convertAndSend(gradebookServiceQueue.getName(), s);
    }

    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T fromJsonString(String str, Class<T> valueType) {
        try {
            return new ObjectMapper().readValue(str, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
