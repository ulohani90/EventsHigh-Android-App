package com.eventshigh.nearme.app.utils;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;

/**
 * See http://developer.android.com/training/cloudsync/backupapi.html.
 */
public class EhBackupAgent extends BackupAgentHelper {
    // The name of the SharedPreferences file
    static final String SHOWCASE_FILENAME = "showcase_internal";

    // A key to uniquely identify the set of backup data
    static final String FILES_BACKUP_KEY = "eh_user_files";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        FileBackupHelper helper = new FileBackupHelper(this, SHOWCASE_FILENAME);
        addHelper(FILES_BACKUP_KEY, helper);
    }
}
