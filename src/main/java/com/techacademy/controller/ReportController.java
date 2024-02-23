package com.techacademy.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.constants.ErrorMessage;

import com.techacademy.entity.Employee;
import com.techacademy.entity.Report;
import com.techacademy.service.EmployeeService;
import com.techacademy.service.ReportService;
import com.techacademy.service.UserDetail;

@Controller
@RequestMapping("reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // 日報一覧画面
    @GetMapping
    public String list(Model model) {

     // ログインユーザーの情報を取得
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // ユーザーがGENERALかADMINかを判定
        if (userDetails instanceof UserDetail) {
            Employee employee = ((UserDetail) userDetails).getEmployee();
            if (employee.getRole() == Employee.Role.GENERAL) {
                // GENERALの場合は、そのユーザーが作成した日報のみを取得して表示
                model.addAttribute("listSize", reportService.findByEmployee(employee).size());
                model.addAttribute("reportList", reportService.findByEmployee(employee));
            } else if (employee.getRole() == Employee.Role.ADMIN) {
                // ADMINの場合は、全ての日報を取得して表示
                model.addAttribute("listSize", reportService.findAll().size());
                model.addAttribute("reportList", reportService.findAll());
            }
        }

        return "reports/list";
    }

    // 日報詳細画面
    @GetMapping(value = "/{id}/")
    public String detail(@PathVariable Long id, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails instanceof UserDetail) {
            Employee employee = ((UserDetail) userDetails).getEmployee();
            model.addAttribute("loggedInUserName", employee.getName());
        }

        model.addAttribute("report", reportService.findByCode(id));
        return "reports/detail";
    }

    // 日報新規登録画面
    @GetMapping(value = "/add")
    public String create(@ModelAttribute Report report,Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails instanceof UserDetail) {
            Employee employee = ((UserDetail) userDetails).getEmployee();
            model.addAttribute("loggedInUserName", employee.getName());
        }

        return "reports/new";
    }

    // 日報新規登録処理
    @PostMapping(value = "/add")
    public String add(@Validated Report report, BindingResult res, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails instanceof UserDetail) {
            Employee employee = ((UserDetail) userDetails).getEmployee();
            report.setEmployee(employee);
            model.addAttribute("loggedInUserName", employee.getName());
        }

        // 入力チェック
        if (res.hasErrors()) {
            return create(report,model);
        }

        // 論理削除を行った従業員番号を指定すると例外となるためtry~catchで対応
        // (findByIdでは削除フラグがTRUEのデータが取得出来ないため)
        try {
            ErrorKinds result = reportService.save(report);

            if (ErrorMessage.contains(result)) {
                model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
                return create(report,model);
            }

        } catch (DataIntegrityViolationException e) {
            model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.DUPLICATE_EXCEPTION_ERROR),
                    ErrorMessage.getErrorValue(ErrorKinds.DUPLICATE_EXCEPTION_ERROR));
            return create(report,model);
        }

        return "redirect:/reports";
    }

    // 日報更新画面
    @GetMapping(value = "{id}/update")
    public String edit(@PathVariable Long id, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails instanceof UserDetail) {
            Employee employee = ((UserDetail) userDetails).getEmployee();
            model.addAttribute("loggedInUserName", employee.getName());
        }

        if(id != null) {
        model.addAttribute("report", reportService.findByCode(id));
        }

        return "reports/update";
    }

    // 日報更新処理
    @PostMapping(value = "{id}/update")
    public String update(@Validated Report report, BindingResult res, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails instanceof UserDetail) {
            Employee employee = ((UserDetail) userDetails).getEmployee();
            report.setEmployee(employee);
            model.addAttribute("loggedInUserName", employee.getName());
        }

        // 入力チェック
        if (res.hasErrors()) {
            return edit(null, model);
        }

        // 論理削除を行った従業員番号を指定すると例外となるためtry~catchで対応
        // (findByIdでは削除フラグがTRUEのデータが取得出来ないため)
        try {
            ErrorKinds result = reportService.update(report);

            if (ErrorMessage.contains(result)) {
                model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
                return edit(null, model);
            }

        } catch (DataIntegrityViolationException e) {
            model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.DUPLICATE_EXCEPTION_ERROR),
                    ErrorMessage.getErrorValue(ErrorKinds.DUPLICATE_EXCEPTION_ERROR));
            return edit(null, model);
        }

        return "redirect:/reports";
    }


    // 日報削除処理
    @PostMapping(value = "/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal UserDetail reportDetail, Model model) {

        ErrorKinds result = reportService.delete(id);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            model.addAttribute("report", reportService.findByCode(id));
            return detail(id, model);
        }

        return "redirect:/reports";
    }

}
