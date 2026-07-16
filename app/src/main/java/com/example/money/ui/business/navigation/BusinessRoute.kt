package com.example.money.ui.business.navigation

/**
 * Business module navigation routes — maps to the UX flow scaffold.
 *
 * @see BusinessNavHost
 * @see docs/business/FIGMA_STRUCTURE.md
 */
sealed class BusinessRoute(val route: String) {

    data object Categories : BusinessRoute("business_categories")

    data object CategoryFlow : BusinessRoute("business_category_flow/{categoryId}") {
        fun create(categoryId: Long) = "business_category_flow/$categoryId"
        const val ARG_CATEGORY_ID = "categoryId"
    }

    data object ProcessBills : BusinessRoute("business_process_bills/{processId}") {
        fun create(processId: Long) = "business_process_bills/$processId"
        const val ARG_PROCESS_ID = "processId"
    }

    data object ProcessCreate : BusinessRoute("business_process_create/{categoryId}") {
        fun create(categoryId: Long) = "business_process_create/$categoryId"
        const val ARG_CATEGORY_ID = "categoryId"
    }

    data object BillCreate : BusinessRoute("business_bill_create/{processId}") {
        fun create(processId: Long) = "business_bill_create/$processId"
        const val ARG_PROCESS_ID = "processId"
    }

    data object Participants : BusinessRoute("business_participants/{processId}") {
        fun create(processId: Long) = "business_participants/$processId"
        const val ARG_PROCESS_ID = "processId"
    }

    data object Debts : BusinessRoute("business_debts/{processId}") {
        fun create(processId: Long) = "business_debts/$processId"
        const val ARG_PROCESS_ID = "processId"
    }

    data object BillDetail : BusinessRoute("business_bill_detail/{billId}") {
        fun create(billId: Long) = "business_bill_detail/$billId"
        const val ARG_BILL_ID = "billId"
    }

    data object MessageComposer : BusinessRoute("business_message_composer/{billId}") {
        fun create(billId: Long) = "business_message_composer/$billId"
        const val ARG_BILL_ID = "billId"
    }

    data object CommunicationCenter : BusinessRoute("business_communication_center")

    data object Automation : BusinessRoute("business_automation")

    data object History : BusinessRoute("business_history")

    data object Templates : BusinessRoute("business_templates")
}
