package com.sumdu.disk.app;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;


import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.Drive;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;


public class DiskOperator {
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME =
            "Drive API Java DiskOperator";

    /**
     * Directory to store user credentials for this application.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/drive-java-quickstart");

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private static final List<String> SCOPES =
            Arrays.asList(DriveScopes.DRIVE);


    private volatile static Drive service;

    private static List<File> filesOnDisk = new ArrayList<>();
    private static Set<String> filter = new HashSet<String>();
    private static Map<String, HashMap<String, String>> browsing = new HashMap<>();
    private static HashMap<String, String> MIMETYPES;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
        try {
            service = getDriveService();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MIMETYPES = new HashMap<String, String>() {
            {
                put("xls", "application/vnd.ms-excel");
                put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                put("xml", "text/xml");
                put("ods", "application/vnd.oasis.opendocument.spreadsheet");
                put("csv", "text/plain");
                put("tmpl", "text/plain");
                put("pdf", "application/pdf");
                put("php", "application/x-httpd-php");
                put("jpg", "image/jpeg");
                put("png", "image/png");
                put("gif", "image/gif");
                put("bmp", "image/bmp");
                put("txt", "text/plain");
                put("doc", "application/msword");
                put("js", "text/js");
                put("swf", "application/x-shockwave-flash");
                put("mp3", "audio/mpeg");
                put("zip", "application/zip");
                put("rar", "application/rar");
                put("tar", "application/tar");
                put("arj", "application/arj");
                put("cab", "application/cab");
                put("html", "text/html");
                put("htm", "text/html");
                put("default", "application/octet-stream");
                put("folder", "application/vnd.google-apps.folder");
            }
        };
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static synchronized Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
                DiskOperator.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Drive client service.
     *
     * @return an authorized Drive client service
     * @throws IOException
     */
    public static synchronized Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }


    private static void printFilesInFolder(Drive service, String folderId)
            throws IOException {
        Drive.Children.List request = service.children().list(folderId);

        do {
            try {
                ChildList children = request.execute();
                File file = service.files().get(folderId).execute();
                //children.getItems().forEach(childReference -> System.out.println(file.getTitle()));
                //System.out.println("Title: " + file.getTitle());

                for (ChildReference child : children.getItems()) {
                    System.out.println("File Id: " + child.getId());

//                    System.out.println("Description: " + file.getDescription());
//                    System.out.println("MIME type: " + file.getMimeType());
                }
                request.setPageToken(children.getNextPageToken());
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);
    }

    /**
     * Retrieve a list of File resources.
     *
     * @return List of File resources.
     */
    private static List<File> retrieveAllFiles() throws IOException {
        List<File> result = new ArrayList<File>();
        Drive.Files.List request = service.files().list();

        do {
            try {
                FileList files = request.execute();

                result.addAll(files.getItems());
                request.setPageToken(files.getNextPageToken());
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);

        return result;
    }


    private static synchronized String getFilePath(Drive drive, File file) {
        String folderPath = "";
        String fullFilePath = null;

        List<ParentReference> parentReferencesList = file.getParents();
        List<String> folderList = new ArrayList<String>();

        List<String> finalFolderList = null;
        try {
            finalFolderList = getFoldersList(drive, parentReferencesList, folderList);

            Collections.reverse(finalFolderList);

            if (finalFolderList.contains("My Drive")) {
                for (String folder : finalFolderList) {
                    folderPath += "/" + folder;
                }

                fullFilePath = folderPath + "/" + file.getTitle();
                return fullFilePath;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static synchronized List<String> getFoldersList(Drive drive, List<ParentReference> parentReferencesList, List<String> folderList) throws IOException {
        for (int i = 0; i < parentReferencesList.size(); i++) {
            String id = parentReferencesList.get(i).getId();

            File file = drive.files().get(id).execute();
            folderList.add(file.getTitle());

            if (!(file.getParents().isEmpty())) {
                List<ParentReference> parentReferenceslist2 = file.getParents();
                getFoldersList(drive, parentReferenceslist2, folderList);
            }
        }
        return folderList;
    }

    public static synchronized String testDiskFiles() throws IOException {
        final StringBuilder response = new StringBuilder();
        response.append("Files on disk:\n");

        retrieveAllFiles().stream().filter(file -> getFilePath(service, file) != null).limit(5).forEach(file -> {
            if (file.getMimeType().contains("folder")) {
                System.out.println(file.getLastModifyingUserName() + " " + file.getFileSize() / 1024f / 1024);
                response.append(getFilePath(service, file)).append("\n\n");
            }

        });
        return response.toString();
    }

    public static synchronized String size() throws IOException {
        About about = service.about().get().execute();
        String result = "";
        float total = about.getQuotaBytesTotal() / 1024f / 1024 / 1024;
        float used = about.getQuotaBytesUsed() / 1024f / 1024 / 1024;
        result += "Total: " + String.format("%.2f", total) + " Gb\n";
        result += "Used: " + String.format("%.2f", used) + "Gb\n";
        result += "Number of files: " + retrieveAllFiles().size();
        return result;
    }

    public static synchronized void numberOfFiles() throws IOException {
        List<File> result = new ArrayList<File>();
        Drive.Files.List request = service.files().list();
        FileList files = request.execute();
        result.addAll(files.getItems());
        result = result.stream().filter(file -> file.getMimeType().contains("folder")).collect(Collectors.toList());
        System.out.println(result.size());
    }


    private static Set<String> printParents(Drive service, String fileId) {
        Set<String> result = new HashSet<>();
        try {

            ParentList parents = service.parents().list(fileId).execute();
            for (ParentReference parent : parents.getItems()) {
                result.add(parent.getId());
                printParents(service, parent.getId());
            }

        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        }
        return result;
    }

    public static void test() throws IOException {
        File root = service.files().get("root").setFields("id").execute();
        System.out.println("ROOT: " + root.getTitle());
        System.out.println(root.getId());
        printFilesInFolder(root.getId());
    }


    public static void printFilesInFolder(String folderId)
            throws IOException {
        Drive.Children.List request = service.children().list(folderId);
        File curr = service.files().get(folderId).execute();
        System.out.println("--------------------- CURRENT FOLDER: " + curr.getTitle() + " " + curr.getId());

        do {
            try {
                ChildList children = request.execute();

                for (ChildReference child : children.getItems()) {
                    File file = service.files().get(child.getId()).execute();
                    System.out.println("CHILD: " + file.getTitle() + "   TYPE:" + file.getMimeType());
                    if (file.getMimeType().contains("folder")) printFilesInFolder(file.getId());
                    //System.out.println("File Id: " + child.getId());
                }
                request.setPageToken(children.getNextPageToken());
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);
    }


    public static String getFilesInFolder(String chatId, String folderId)
            throws IOException {
        Drive.Children.List request = service.children().list(folderId).setQ("trashed = false");
        File curr = service.files().get(folderId).execute();
        StringBuilder result = new StringBuilder();
        HashMap<String, String> filesInFolder = new HashMap<>();

        do {
            try {
                ChildList children = request.execute();


                for (ChildReference child : children.getItems()) {
                    File file = service.files().get(child.getId()).execute();
                    filesInFolder.put(file.getTitle(), file.getId());
                    result.append(file.getMimeType().contains("folder") ? file.getTitle() + "/\n" : file.getTitle() + "\n");
                }
                request.setPageToken(children.getNextPageToken());
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);
        browsing.put(chatId, filesInFolder);
        return result.toString();
    }

    public static List<String> getFilesInFolder(String chatId, String folderId, boolean b)
            throws IOException {
        ArrayList<String> files = new ArrayList<>();
        Drive.Children.List request = service.children().list(folderId).setQ("trashed = false");
        HashMap<String, String> filesInFolder = new HashMap<>();
        do {
            try {
                ChildList children = request.execute();
                for (ChildReference child : children.getItems()) {
                    File file = service.files().get(child.getId()).execute();
                    System.out.println("SEEING FILE: " + file.getTitle());
                    filesInFolder.put(file.getTitle(), file.getId());
                    files.add(file.getMimeType().contains("folder") ? "//" +file.getTitle() + "/\n" : "/file: " + file.getTitle() + "\n");
                }
                request.setPageToken(children.getNextPageToken());
            } catch (IOException e) {
                System.out.println("An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);
        browsing.put(chatId, filesInFolder);
        return files;
    }

    public static List<String> browse(String chatId, String folderName, boolean b) {
        //if (folderName == null || folderName.length() == 0) return "Error. Not valid folder name";
        HashMap<String, String> folders = browsing.get(chatId);

        System.out.println("Q BROWSE");
        System.out.println("SEARCHING FOLDERNAME: " + folderName);
        File currentFolder;
        try {
            if (folders == null || folderName == null || folderName.length() == 0) {
                currentFolder = service.files().get("root").setFields("id").execute();
                System.out.println("GETTING ROOT FOLDER");
                return getFilesInFolder(chatId, currentFolder.getId(), b);
            } else {
                String targetFolderId = folders.get(folderName);
                if (targetFolderId == null) return null;
                File file = service.files().get(targetFolderId).execute();
                System.out.println("GETTING WHATS IN FOLDER " + file.getTitle());
                return getFilesInFolder(chatId, targetFolderId, b);
            }
            //FileList allFiles = service.files().list().setQ("trashed = false").execute();
        } catch (IOException e) {
            System.out.println("Error occurred " + e);
        }
        return null;
    }


    public static String browse(String chatId, String folderName) {
        //if (folderName == null || folderName.length() == 0) return "Error. Not valid folder name";
        HashMap<String, String> folders = browsing.get(chatId);

        File currentFolder;
        try {
            if (folders == null || folderName == null || folderName.length() == 0) {
                currentFolder = service.files().get("root").setFields("id").execute();
                return getFilesInFolder(chatId, currentFolder.getId());
            } else {
                String targetFolderId = folders.get(folderName);
                if (targetFolderId == null) return "Not valid folder name";
                File file = service.files().get(targetFolderId).execute();
                return getFilesInFolder(chatId, targetFolderId);
            }
            //FileList allFiles = service.files().list().setQ("trashed = false").execute();
        } catch (IOException e) {
            System.out.println("Error occurred " + e);
        }
        return "Error. Cannot process the request";
    }

    public static String upBrowse(String chatId) {
        HashMap<String, String> folders = browsing.get(chatId);
        try {
            if (folders == null || folders.size() == 0) return "Error. Cannot go to upper folder from here.";
            Map.Entry<String, String> entry = folders.entrySet().iterator().next();
            File file = service.files().get(entry.getValue()).execute();
            if (file.getParents() == null || file.getParents().size() == 0)
                return "This is a top folder";
            File current = service.files().get(file.getParents().get(0).getId()).execute();
            if (current.getParents() == null || current.getParents().size() == 0)
                return "This is a top folder.";
            File parent = service.files().get(current.getParents().get(0).getId()).execute();

            return getFilesInFolder(chatId, parent.getId());
        } catch (Exception e) {
            System.out.println("Error occurred " + e);
        }
        return "Error. Cannot process the request";
    }

    public static List<String> upBrowse(String chatId, boolean f) {
        HashMap<String, String> folders = browsing.get(chatId);
        try {
            if (folders == null || folders.size() == 0) return null;
            Map.Entry<String, String> entry = folders.entrySet().iterator().next();
            File file = service.files().get(entry.getValue()).execute();
            if (file.getParents() == null || file.getParents().size() == 0)
                return null;
            File current = service.files().get(file.getParents().get(0).getId()).execute();
            if (current.getParents() == null || current.getParents().size() == 0)
                return null;
            File parent = service.files().get(current.getParents().get(0).getId()).execute();

            return getFilesInFolder(chatId, parent.getId(), f);
        } catch (Exception e) {
            System.out.println("Error occurred " + e);
        }
        return null;
    }


    public static File insertFile(java.io.File insert, String uploadFolderId) {
        String mimeType = getMimeTypeFromExtension(getFileExtension(insert));
        // File's metadata.
        File body = new File();
        body.setTitle(insert.getName());
        body.setMimeType(mimeType);
        if (uploadFolderId != null && uploadFolderId.length() > 30) {
            body.setParents(Collections.singletonList(new ParentReference().setId(uploadFolderId)));
        }

        // File's content.
        java.io.File fileContent = new java.io.File(insert.getAbsolutePath());
        System.out.println("extension of file is " + getFileExtension(fileContent));
        FileContent mediaContent = new FileContent(mimeType, fileContent);
        try {
            File file = service.files().insert(body, mediaContent).execute();
            return file;
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
            return null;
        }
    }

    public static String getMimeTypeFromExtension(String extension) {
        return MIMETYPES.get(extension);
    }

    private static String getFileExtension(java.io.File file) {
        String extension = "";

        try {
            if (file != null && file.exists()) {
                String name = file.getName();
                extension = name.substring(name.lastIndexOf(".") + 1);
            }
        } catch (Exception e) {
            extension = "";
        }
        return extension;
    }


    public static java.io.File downloadFile(String chatId, String filename, String localPath) {
        String fileId = browsing.get(chatId).get(filename);
        try {
            File file = service.files().get(fileId).execute();
            if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
                try {
                    HttpResponse resp = service.getRequestFactory()
                            .buildGetRequest(new GenericUrl(file.getDownloadUrl()))
                            .execute();
                    java.io.File newFile = new java.io.File(localPath + "\\" + file.getTitle());
                    FileUtils.copyInputStreamToFile(resp.getContent(), newFile);
                    return newFile;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String checkFileSize(String chatId, String filename) {
        String fileId = browsing.get(chatId).get(filename);
        try {
            File file = service.files().get(fileId).execute();
            if (file.getFileSize() == null || file.getFileSize() == 0) return "Couldn't process remote file.";
            if (file.getFileSize() / 1024f / 1024 > 50) {
                return "File is too big ("
                        + String.format("%.2f", file.getFileSize() / 1024f / 1024)
                        + " Mb), try downloading it using a web browser.";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Couldn't process remote file.";
        }
        return "ok";
    }

}
