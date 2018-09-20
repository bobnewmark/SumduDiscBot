package com.sumdu.disk.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.Arrays;

@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties.class)
public class SumDuDiskApp {

    private static long SLEEP_TIME = 5000L;
    private static ReplyKeyboardMarkup markup;


    @Autowired
    private ConfigProperties configProps;

    @Bean
    public TelBot firstBot() {
        return new TelBot(this.configProps.getToken(),
                this.configProps.getUsername(),
                this.configProps.getLocalFilePath(),
                this.configProps.getUploadFolderId());
    }

    @Bean
    public CommandLineRunner commandLineRunner(TelBot bot, ApplicationContext ctx) throws Exception {
        return args -> {

            System.out.println("Let's inspect the beans provided by Spring Boot:");

            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                System.out.println(beanName);
            }

            while (true) {
                bot.getChatIdsCurrentlyConnectedToMe()
                        .forEach(chatId -> {
                            try {
                                if (bot.getText().startsWith("Hello")) {
                                    SendMessage message = new SendMessage().setChatId(chatId).setText(bot.getText());
                                    message.setReplyMarkup(null);
                                    bot.sendMessage(message);
                                    bot.setText("");
                                } else if (getMarkup() != null) {
                                    SendMessage message = new SendMessage().setChatId(chatId);
                                    message.setReplyMarkup(getMarkup());
                                    bot.sendMessage(message);
                                    setMarkup(null);
                                }else if (!"".equals(bot.getText()) && !bot.eraseText()) {
                                    SendMessage message = new SendMessage().setChatId(chatId).setText(bot.getText());
                                    message.setReplyMarkup(null);
                                    bot.sendMessage(message);
                                } else if (!"".equals(bot.getText()) && bot.eraseText()) {
                                    SendMessage message = new SendMessage().setChatId(chatId).setText(bot.getText());
                                    message.setReplyMarkup(null);
                                    bot.sendMessage(message);
                                    bot.setText("");
                                }
                            }
                            catch (TelegramApiException e) {
                                e.printStackTrace();
                                bot.removeMeFromChat(chatId);
                            }
                        });
                Thread.sleep(SLEEP_TIME);
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(SumDuDiskApp.class, args);
    }

    @Component
    public class ServerInitializer implements ApplicationRunner {

        @Override
        public void run(ApplicationArguments applicationArguments) throws Exception {

            //DiskOperator.testAllFiles();
            //ActivityMonitor.test();
            //DiskOperator.parentsOneMoreTest();
            //DiskOperator.printFilesInFolder("1khp3BJ_uiZ2KIuXD4cNMnbL-riD2dVWr");
            //System.out.println("DONE");
            //DiskOperator.test();
            //DiskOperator.uploadTest();

        }
    }

    public static long getSleepTime() {
        return SLEEP_TIME;
    }

    public static void setSleepTime(long sleepTime) {
        SLEEP_TIME = sleepTime;
    }

    public static ReplyKeyboardMarkup getMarkup() {
        return markup;
    }

    public static void setMarkup(ReplyKeyboardMarkup markup) {
        SumDuDiskApp.markup = markup;
    }
}
