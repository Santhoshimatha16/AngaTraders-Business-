
package com.angatraders.backend.service;

import com.angatraders.backend.entity.Company;
import com.angatraders.backend.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Company getCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Company not found with ID: " + id));
    }

    public Company createCompany(Company company) {
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());

        return companyRepository.save(company);
    }

    public Company updateCompany(Long id, Company updatedCompany) {
        Company existingCompany = getCompanyById(id);

        existingCompany.setCompanyName(updatedCompany.getCompanyName());
        existingCompany.setAddress(updatedCompany.getAddress());
        existingCompany.setContactPerson(updatedCompany.getContactPerson());
        existingCompany.setGstNumber(updatedCompany.getGstNumber());
        existingCompany.setLocation(updatedCompany.getLocation());
        existingCompany.setPhone(updatedCompany.getPhone());
        existingCompany.setStatus(updatedCompany.getStatus());
        existingCompany.setNotes(updatedCompany.getNotes());
        existingCompany.setUpdatedAt(LocalDateTime.now());

        return companyRepository.save(existingCompany);
    }

    public void deleteCompany(Long id) {
        Company company = getCompanyById(id);
        companyRepository.delete(company);
    }
}
