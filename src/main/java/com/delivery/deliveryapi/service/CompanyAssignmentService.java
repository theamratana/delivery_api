package com.delivery.deliveryapi.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.model.Employee;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.repo.EmployeeRepository;
import com.delivery.deliveryapi.repo.PendingEmployeeRepository;
import com.delivery.deliveryapi.repo.UserRepository;

@Service
public class CompanyAssignmentService {

    private final PendingEmployeeRepository pendingEmployeeRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public CompanyAssignmentService(PendingEmployeeRepository pendingEmployeeRepository,
                                   UserRepository userRepository,
                                   EmployeeRepository employeeRepository) {
        this.pendingEmployeeRepository = pendingEmployeeRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Handles automatic company assignment during phone verification.
     * Checks for pending employee records and assigns the user to the company.
     *
     * @param user The user to potentially assign to a company
     * @param phoneNumber The verified phone number
     */
    @Transactional
    public void handlePhoneVerificationAssignment(User user, String phoneNumber) {
        // Only assign if user has no company and phone number is provided
        if (user.getCompany() == null && phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            var pendingEmployeeOpt = pendingEmployeeRepository.findActiveByPhone(phoneNumber.trim(), Instant.now());
            if (pendingEmployeeOpt.isPresent()) {
                var pendingEmployee = pendingEmployeeOpt.get();

                // Assign user to company
                user.setCompany(pendingEmployee.getCompany());
                user.setUserRole(pendingEmployee.getRole());
                user.setIncomplete(false);
                userRepository.save(user);

                // Create employee record for profile sync
                Employee employee = new Employee(user, pendingEmployee.getCompany(), pendingEmployee.getRole());
                employeeRepository.save(employee);

                // Remove the pending employee record
                pendingEmployeeRepository.delete(pendingEmployee);
            }
        }
    }
}