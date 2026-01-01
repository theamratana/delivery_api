package com.delivery.deliveryapi.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.deliveryapi.model.Employee;
import com.delivery.deliveryapi.model.User;
import com.delivery.deliveryapi.model.UserType;
import com.delivery.deliveryapi.repo.DistrictRepository;
import com.delivery.deliveryapi.repo.EmployeeRepository;
import com.delivery.deliveryapi.repo.PendingEmployeeRepository;
import com.delivery.deliveryapi.repo.ProvinceRepository;
import com.delivery.deliveryapi.repo.UserRepository;

@Service
public class CompanyAssignmentService {

    private final PendingEmployeeRepository pendingEmployeeRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;

    public CompanyAssignmentService(PendingEmployeeRepository pendingEmployeeRepository,
                                   UserRepository userRepository,
                                   EmployeeRepository employeeRepository,
                                   ProvinceRepository provinceRepository,
                                   DistrictRepository districtRepository) {
        this.pendingEmployeeRepository = pendingEmployeeRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
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
                user.setUserType(UserType.COMPANY);
                
                // Apply profile information from pending employee
                if (pendingEmployee.getFirstName() != null) {
                    user.setFirstName(pendingEmployee.getFirstName());
                }
                if (pendingEmployee.getLastName() != null) {
                    user.setLastName(pendingEmployee.getLastName());
                }
                if (pendingEmployee.getDisplayName() != null) {
                    user.setDisplayName(pendingEmployee.getDisplayName());
                }
                if (pendingEmployee.getEmail() != null) {
                    user.setEmail(pendingEmployee.getEmail());
                }
                if (pendingEmployee.getAddress() != null) {
                    user.setDefaultAddress(pendingEmployee.getAddress());
                }
                // Validate province/district IDs exist before assigning (foreign key constraint)
                if (pendingEmployee.getDefaultProvinceId() != null 
                    && provinceRepository.existsById(pendingEmployee.getDefaultProvinceId())) {
                    user.setDefaultProvinceId(pendingEmployee.getDefaultProvinceId());
                }
                if (pendingEmployee.getDefaultDistrictId() != null 
                    && districtRepository.existsById(pendingEmployee.getDefaultDistrictId())) {
                    user.setDefaultDistrictId(pendingEmployee.getDefaultDistrictId());
                }
                
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