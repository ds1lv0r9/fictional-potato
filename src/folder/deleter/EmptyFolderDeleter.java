
package folder.deleter;

import java.io.*;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;


public class EmptyFolderDeleter
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        File folder;

        if (args.length == 0)
        {
            System.out.println("Correct usage is: java -jar folder_deleter.jar \"path to folder\"");
            exit(1);
        }
        folder = new File(args[0]);

        List<File> emptyDirectories = new ArrayList<>();

        findEmpty(folder, emptyDirectories);

        List<String> logEntries = deleteFoldersAndCreateLog(emptyDirectories);

        saveLogFiles(logEntries, folder);
    }

    private static boolean findEmpty(File directory, List<File> emptyFolders)
    {
        List<Boolean> subsEmpty = new ArrayList<>();
        boolean isDirectoryConsideredEmpty = true;
        File[] folderContents = directory.listFiles();

        if (!Objects.isNull(folderContents) && folderContents.length != 0)
        {
            // Iterate through every file/folder
            for (File content : folderContents)
                if (content.isDirectory())
                    // check if this folder is empty
                    subsEmpty.add(findEmpty(content, emptyFolders));
                else
                {
                    // if folder contains a file then it is not empty
                    subsEmpty.add(false);
                    break;
                }

            for (Boolean isSubDirectoryEmpty : subsEmpty)
                if (!isSubDirectoryEmpty)
                {
                    isDirectoryConsideredEmpty = false;
                    break;
                }
        }

        if (isDirectoryConsideredEmpty)
            emptyFolders.add(directory);

        return isDirectoryConsideredEmpty;
    }

    private static List<String> deleteFoldersAndCreateLog(List<File> emptyFolders)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date nowDate;

        List<String> logEntries = new ArrayList<>();
        Iterator<File> emptyIterator = emptyFolders.iterator();
        String path;

        while (emptyIterator.hasNext())
        {
            path = emptyIterator.next().getPath();
            File currentFile = new File(path);
            String message;
            nowDate = new Date();
            if (currentFile.delete())
                message = "[" + dateFormat.format(nowDate) + "] Deleted " + path;
            else
                message = "[" + dateFormat.format(nowDate) + "] Couldn't delete " + path;
            logEntries.add(message);
        }

        return logEntries;
    }

    private static void saveLogFiles(List<String> logEntries, File directory)
    {
        File logDirectory = null;
        try
        {
            logDirectory = new File(directory.getCanonicalPath() + File.separator + ".log");
        }
        catch (IOException ex)
        {
            Logger.getLogger(EmptyFolderDeleter.class.getName()).log(Level.SEVERE, null, ex);
        }

        DateFormat fileNameDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        Date date = new Date();

        if (logDirectory != null && !logEntries.isEmpty())
        {
            logDirectory.mkdirs();

            File latestLog, archivedLog, archive;
            PrintWriter output;
            try
            {
                archive = new File(logDirectory.getCanonicalPath() + File.separator + "archive");
                archive.mkdirs();

                latestLog = new File(logDirectory.getCanonicalPath() + File.separator + "latest.log");
                output = new PrintWriter(new BufferedWriter(new FileWriter(latestLog, false)));

                for (String logEntry : logEntries)
                    output.println(logEntry);
                output.close();

                archivedLog = new File(logDirectory.getCanonicalPath() + File.separator + "archive" + File.separator + fileNameDateFormat.format(date) + ".log");
                copyFile(latestLog, archivedLog);
            }
            catch (IOException e)
            {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void copyFile(File sourceFile, File destFile)
    {
        try
        {
            if (!destFile.exists())
                destFile.createNewFile();

            try (FileChannel source = new FileInputStream(sourceFile).getChannel(); FileChannel destination = new FileOutputStream(destFile).getChannel())
            {
                destination.transferFrom(source, 0, source.size());
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
