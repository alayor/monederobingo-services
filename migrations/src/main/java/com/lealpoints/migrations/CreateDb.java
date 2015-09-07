package com.lealpoints.migrations;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.lealpoints.db.DataSourceFactory;
import com.lealpoints.migrations.util.DBUtil;
import org.apache.commons.io.FileUtils;

public class CreateDb {

    public static void main(String[] args) throws Exception {
        System.out.println("Creating database...");
        CreateDb.run();
        System.out.println("Database create successfully.");
        Migrate.main(null);
    }

    private static void run() throws Exception {
        File file = new File("scripts/createdb.sql");
        String sql = FileUtils.readFileToString(file);
        Connection connection = DataSourceFactory.getDevDataSource().getConnection();
        DBUtil.executeSql(sql, connection);
        runSetupScripts();
    }

    private static void runSetupScripts() throws Exception {
        final File[] scripts = loadSetupScripts();
        for (File script : scripts) {
            System.out.println(script.getName());
            DBUtil.executeScript(script, DataSourceFactory.getDevDataSource().getConnection());
            DBUtil.executeScript(script, DataSourceFactory.getUnitTestDataSource().getConnection());
            DBUtil.executeScript(script, DataSourceFactory.getFunctionalTestDataSource().getConnection());
        }
    }

    private static File[] loadSetupScripts() {
        File dir = new File("scripts/setup");
        File[] filesArray = dir.listFiles();
        List<File> filesFromSetup = new ArrayList<>();
        if (filesArray != null) {
            filesFromSetup = Arrays.asList(filesArray);
        }
        final File[] totalArrayFiles = new File[filesFromSetup.size()];
        filesFromSetup.toArray(totalArrayFiles);
        return totalArrayFiles;
    }
}
