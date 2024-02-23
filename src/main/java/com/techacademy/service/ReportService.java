package com.techacademy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.entity.Employee;
import com.techacademy.entity.Report;
import com.techacademy.repository.EmployeeRepository;
import com.techacademy.repository.ReportRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ReportService(ReportRepository reportRepository, PasswordEncoder passwordEncoder) {
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 日報保存
    @Transactional
    public ErrorKinds save(Report report) {

        // 日付重複チェック
     // 1. 保存する日報の日付を取得
        LocalDate reportDate = report.getReportDate();

        // 2. 同じ従業員に関連付けられたすべての日報を取得
        Employee employee = report.getEmployee();
        List<Report> existingReports = reportRepository.findByEmployee(employee);

        // 3. 取得した日報の中から、日付が重複しているかどうかを確認
        for (Report existingReport : existingReports) {
            if (existingReport.getReportDate().equals(reportDate)) {
                // 重複している場合はエラーを返す
                return ErrorKinds.DATECHECK_ERROR;
            }
        }


        report.setEmployee(employee);

        report.setDeleteFlg(false);

        LocalDateTime now = LocalDateTime.now();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        reportRepository.save(report);
        return ErrorKinds.SUCCESS;
    }


    // 日報更新
    @Transactional
    public ErrorKinds update(Report report) {
     // 日付重複チェック
        // 1. 保存する日報の日付を取得
           LocalDate reportDate = report.getReportDate();

           // 2. 同じ従業員に関連付けられたすべての日報を取得
           Employee employee = report.getEmployee();
           List<Report> existingReports = reportRepository.findByEmployee(employee);

           // 3. 現在保存しようとしている日報をexistingReportsから除外する
           existingReports = existingReports.stream()
               .filter(existingReport -> !existingReport.getId().equals(report.getId())) // IDが同じでないものだけを残す
               .collect(Collectors.toList());

           // 4. 取得した日報の中から、日付が重複しているかどうかを確認
           for (Report existingReport : existingReports) {
               if (existingReport.getReportDate().equals(reportDate)) {
                   // 重複している場合はエラーを返す
                   return ErrorKinds.DATECHECK_ERROR;
               }
           }

        Report existingReport = this.findByCode(report.getId());
        report.setCreatedAt(existingReport.getCreatedAt());
        report.setEmployee(existingReport.getEmployee());

        report.setDeleteFlg(false);

        LocalDateTime now = LocalDateTime.now();
        report.setUpdatedAt(now);

        reportRepository.save(report);
        return ErrorKinds.SUCCESS;
    }

    // 日報削除
    @Transactional
    public ErrorKinds delete(Long id) {

        Report report = findByCode(id);
        reportRepository.delete(report);

        return ErrorKinds.SUCCESS;
    }



    // 日報一覧表示処理
    public List<Report> findAll() {
        return reportRepository.findAll();
    }

    // 1件を検索
    public Report findByCode(Long id) {
        // findByIdで検索
        Optional<Report> option = reportRepository.findById(id);
        // 取得できなかった場合はnullを返す
        Report report = option.orElse(null);
        return report;
    }

    public List<Report> findByEmployee(Employee employee) {
        return reportRepository.findByEmployee(employee);
    }



}
