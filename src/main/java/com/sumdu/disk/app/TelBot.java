package com.sumdu.disk.app;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.objects.Document;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * A Telegram polling bot
 */
public class TelBot extends TelegramLongPollingBot {

    private String botToken;
    private String botUsername;
    private String localFilePath;
    private String uploadFolderId;
    private String text;
    private ArrayList<String> bigText = new ArrayList<>();
    private AtomicLong chatId = new AtomicLong(-1L);
    private Queue<Long> chatIds = new ConcurrentLinkedQueue<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(TelBot.class);
    private boolean eraseText = false;
    private static final long DEFAULT_SLEEP_TIME = 5000L;
    private static final long QUICK_RESPONSE = 3000L;
    private static long user_default;

    public TelBot(String botToken, String botUsername, String localFilePath, String uploadFolderId) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.localFilePath = localFilePath;
        this.uploadFolderId = uploadFolderId;
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update != null && update.getMessage() != null && update.getMessage().hasText()) {
            if ("/pause".equals(update.getMessage().getText())) {
                removeMeFromChat(update.getMessage().getChatId());
            } else if ("/changes".equals(update.getMessage().getText())) {
                text = "";
                try {
                    text = ActivityMonitor.getChanges();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if ("/size".equals(update.getMessage().getText())) {
                text = "";
                try {
                    //TODO add counting number of files
                    DiskOperator.numberOfFiles();
                    text = DiskOperator.size();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if ("/help".equals(update.getMessage().getText())) {
                text = "/browse - browse folders on Google Drive\n" +
                        "/size - total and free size on disk, number of files\n" +
                        "/changes" +
                        "/pause - stop receiving messages";
            } else if ("/settings".equals(update.getMessage().getText())) {
                text = "";
                //TODO
            } else if ("/browse".equals(update.getMessage().getText())) {
                text = "";
                List<String> folders = DiskOperator.browse(String.valueOf(update.getMessage().getChatId()), null, false);
                SumDuDiskApp.setMarkup(buildButtonsFromList(folders));
                eraseText = true;
                SumDuDiskApp.setSleepTime(QUICK_RESPONSE);
            } else if (update.getMessage().getText().startsWith("//")) {
                text = "";
                String folderName = update.getMessage().getText().substring(2);
                List<String> folders =  DiskOperator.browse(String.valueOf(update.getMessage().getChatId()), folderName.substring(0, folderName.length()-1), false);
                SumDuDiskApp.setMarkup(buildButtonsFromList(folders));
                eraseText = true;
            } else if ("..".equals(update.getMessage().getText())) {
                text = "";
                List<String> folders = DiskOperator.upBrowse(String.valueOf(update.getMessage().getChatId()), true);
                SumDuDiskApp.setMarkup(buildButtonsFromList(folders));
            } else if ("/bot".equals(update.getMessage().getText())) {
                text = "";
                text += "uploadFolderId: " + uploadFolderId + "\n";
                for (Object o : getChatIdsCurrentlyConnectedToMe().toArray()) {
                    text += o.toString() + ", ";
                }
            } else if (update.getMessage().getText().startsWith("/file:") && update.getMessage().getText().length() > 4) {
                text = "";

                String s = update.getMessage().getText();
                String sizeCheckingResult = DiskOperator.checkFileSize(String.valueOf(update.getMessage().getChatId()), s.substring(s.indexOf("/")+7));
                if (!"ok".equals(sizeCheckingResult)) {
                    text = sizeCheckingResult;
                } else {
                    File file = DiskOperator.downloadFile(String.valueOf(update.getMessage().getChatId()), s.substring(s.indexOf("/")+7), getLocalFilePath());
                    if (file != null) {
                        sendDocUploadingAFile(update.getMessage().getChatId(), file);
                    } else text = "Error downloading the file.";
                }
            } else if ("/test_keyboard".equals(update.getMessage().getText())) {

                // Create ReplyKeyboardMarkup object
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                // Create the keyboard (list of keyboard rows)
                List<KeyboardRow> keyboard = new ArrayList<>();
                // Create a keyboard row
                KeyboardRow row = new KeyboardRow();
                // Set each button, you can also use KeyboardButton objects if you need something else than text
                row.add("Row 1 Button 1");
                //row.add("Row 1 Button 2");
                //row.add("Row 1 Button 3");
                // Add the first row to the keyboard
                keyboard.add(row);
                // Create another keyboard row
                row = new KeyboardRow();
                // Set each button for the second line
                row.add("Row 2 Button 1");
                //row.add("Row 2 Button 2");
                //row.add("Row 2 Button 3");
                // Add the second row to the keyboard
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 3 Button 1");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 4 Button 1");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 5 Button 1");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 6 Button 1");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 7 Button 1");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 8 Button 1");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 9 Button 1");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 10 Button 1");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 11 Button 1");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 12 Button 1");
                keyboard.add(row);
                // Set the keyboard to the markup
                keyboardMarkup.setOneTimeKeyboard(true);
                keyboardMarkup.setKeyboard(keyboard);
                SumDuDiskApp.setMarkup(keyboardMarkup);
            } else if ("/clear".equals(update.getMessage().getText())) {
                text = "";
                clearChats();
            } else {
                text = "Hello, enter: \"/help\" to see list of commands (alpha version).";
                LOGGER.info("Received a chat bot update from Telegram servers: {}", update);
                this.chatIds.offer(update.getMessage().getChatId());
            }
        } else if (update != null && update.getMessage() != null && update.getMessage().getPhoto() != null) {
            List<PhotoSize> photos = update.getMessage().getPhoto();
            PhotoSize biggest = photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null);
            uploadPhoto(biggest);
            text = "image was uploaded to the upload folder.";

        } else if (update != null && update.getMessage() != null && update.getMessage().getDocument() != null) {
            Document doc = update.getMessage().getDocument();
            uploadDocument(doc);
            text = "document was uploaded to the upload folder.";
        }
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }

    public Stream<Long> getChatIdsCurrentlyConnectedToMe() {
        return this.chatIds.stream();
    }

    public void removeMeFromChat(Long chatId) {
        this.chatIds.remove(chatId);
    }

    public String getText() {
        return text;
    }

    public void setText(String s) {
        this.text = s;
    }

    public boolean eraseText() {
        return eraseText;
    }

    public void eraseText(boolean eraseText) {
        this.eraseText = eraseText;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public String getUploadFolderId() {
        return uploadFolderId;
    }

    public void setUploadFolderId(String uploadFolderId) {
        this.uploadFolderId = uploadFolderId;
    }

    public void clearChats() {
        if (getChatIdsCurrentlyConnectedToMe().toArray().length > 1) {
            for (Object o : getChatIdsCurrentlyConnectedToMe().toArray()) {
                Long r = (Long) o;
                System.out.println("Chat: " + r);
                removeMeFromChat(r);
            }
        }
    }

    private void splitBigText() {
        int start = 0;
        int end = 0;
        String temp = "";
        while (!text.equals("")) {
            start = end;
            end = text.substring(start, 4000).lastIndexOf("\n\n");
            bigText.add(text.substring(start, end));
        }
    }

    public String uploadPhoto(PhotoSize photo) {
        String filePath = null;
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(photo.getFileId());
        try {
            org.telegram.telegrambots.api.objects.File file = execute(getFileMethod);
            String uploadedFileId = photo.getFileId();
            GetFile uploadedFile = new GetFile();
            uploadedFile.setFileId(uploadedFileId);
            try {
                String uploadedFilePath = getFile(uploadedFile).getFilePath();
                File localFile = new File("/" + file.getFilePath().substring(file.getFilePath().lastIndexOf("/") + 1));
                InputStream is = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + uploadedFilePath).openStream();
                FileUtils.copyInputStreamToFile(is, localFile);
                filePath = localFile.getAbsolutePath();
                System.out.println("photo was downloaded to " + localFile.getAbsolutePath());
                DiskOperator.insertFile(localFile, uploadFolderId);
                localFile.delete();
            } catch (TelegramApiException | IOException e) {
                e.printStackTrace();
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    public String uploadDocument(Document doc) {
        String filePath = null;
        String uploadedFileId = doc.getFileId();
        GetFile uploadedFile = new GetFile();
        uploadedFile.setFileId(uploadedFileId);
        System.out.println("LOCAL FILE PATH: " + localFilePath);
        try {
            String uploadedFilePath = getFile(uploadedFile).getFilePath();
            File localFile = new File("/" + doc.getFileName());
            InputStream is = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + uploadedFilePath).openStream();
            FileUtils.copyInputStreamToFile(is, localFile);
            System.out.println("file was downloaded to " + localFile.getAbsolutePath());
            filePath = localFile.getAbsolutePath();
            DiskOperator.insertFile(localFile, uploadFolderId);
            localFile.delete();
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }


    private void sendDocUploadingAFile(Long chatId, java.io.File save) {
        SendDocument sendDocumentRequest = new SendDocument();
        sendDocumentRequest.setChatId(chatId);
        sendDocumentRequest.setNewDocument(save);
        sendDocumentRequest.setCaption("file downloaded by SumduDiscBot");
        try {
            sendDocument(sendDocumentRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        save.delete();
    }

    private ReplyKeyboardMarkup buildButtonsFromList (List<String> names) {
        System.out.println("BUILDING KEYBOARD");
        if (names == null) return null;
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow goUpButton = new KeyboardRow();
        goUpButton.add("..");
        keyboard.add(goUpButton);
        names.forEach( s -> {
            KeyboardRow row = new KeyboardRow();
            System.out.println("BUTTON: " + s);
            row.add(s);
            keyboard.add(row);
        });
        keyboardMarkup.setOneTimeKeyboard(true);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }




}
