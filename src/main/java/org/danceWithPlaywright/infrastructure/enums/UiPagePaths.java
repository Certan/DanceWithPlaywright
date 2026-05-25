package org.danceWithPlaywright.infrastructure.enums;

import lombok.Getter;

@Getter
public enum UiPagePaths {
    ADMIN_LOGIN("/web/index.php/auth/login"),

    // PIM page paths
    PIM_CONFIGURATION_OPTIONAL_FIELDS("/web/index.php/pim/configurePim"),
    PIM_CONFIGURATION_CUSTOM_FIELDS("/web/index.php/pim/listCustomFields"),
    PIM_CONFIGURATION_DATA_IMPORT("/web/index.php/pim/pimCsvImport"),
    PIM_REPORTING_METHODS("/web/index.php/pim/viewReportingMethods"),
    PIM_REPORTING_TERMINATION_REASONS("/web/index.php/pim/viewTerminationReasons"),
    PIM_ADD_EMPLOYEE("/web/index.php/pim/addEmployee"),
    PIM_EMPLOYEE_LIST("/web/index.php/pim/viewEmployeeList"),
    PIM_REPORTS("/web/index.php/pim/viewDefinedPredefinedReports"),

    // DASHBOARD
    DASHBOARD("/web/index.php/dashboard/index");


    private final String path;

    UiPagePaths(String path){
        this.path = path;
    }
}
