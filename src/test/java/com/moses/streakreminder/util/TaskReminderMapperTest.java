package com.moses.streakreminder.util;

import com.google.cloud.firestore.DocumentSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskReminderMapperTest {

    @Test
    void mapsLegacyTaskDocumentsToNotificationDisabledDefaults() {
        var document = mock(DocumentSnapshot.class);
        when(document.exists()).thenReturn(true);
        when(document.getId()).thenReturn("legacy-task");
        when(document.getString(anyString())).thenReturn(null);
        when(document.getBoolean(anyString())).thenReturn(null);
        when(document.get(anyString())).thenReturn(null);

        var task = FirestoreMapper.toTaskReminder(document);

        assertEquals("legacy-task", task.getId());
        assertFalse(task.isAllDay());
        assertFalse(task.isCompleted());
        assertFalse(task.isNotificationEnabled());
        assertEquals(0, task.getReminderMinutesBefore());
        assertNull(task.getReminderStatus());
    }
}
