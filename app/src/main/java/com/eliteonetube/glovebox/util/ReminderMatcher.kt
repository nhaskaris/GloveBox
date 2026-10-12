package com.eliteonetube.glovebox.util

/** Decides whether a logged service type fulfils a reminder, e.g. "Oil Change" closes "Oil change". */
object ReminderMatcher {
    private const val MIN_LENGTH = 3

    fun matches(reminderDescription: String, serviceType: String): Boolean {
        val reminder = reminderDescription.trim().lowercase()
        val service = serviceType.trim().lowercase()
        if (reminder.length < MIN_LENGTH || service.length < MIN_LENGTH) return false
        return reminder.contains(service) || service.contains(reminder)
    }
}
