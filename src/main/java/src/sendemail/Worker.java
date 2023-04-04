package src.sendemail;

import java.io.File;
import java.util.*;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


@SpringBootApplication
@EnableZeebeClient
public class Worker {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private Environment environment;

    @Value("${sender.email}")
    private String sender;

    @Value("${receiver.email}")
    private String receiver;

    public static void main(String[] args) {
        SpringApplication.run(Worker.class, args);
    }

    @JobWorker(type = "SendEmail")
    public void SendEmail(final JobClient client, final ActivatedJob job) throws MessagingException {
        Map<String, Object> variablesAsMap = job.getVariablesAsMap();

        String subject = (String) variablesAsMap.get("subject");
        String body = (String) variablesAsMap.get("body");

        String[] filePathArray = variablesAsMap.get("attachment").toString().split(",");

        if (variablesAsMap.get("sender") != null && !((String) variablesAsMap.get("sender")).isEmpty()) {
            if (ValidateEmail.validateEmail((String) variablesAsMap.get("sender"))) {
                sender = variablesAsMap.get("sender").toString();
            }
        }

        if (variablesAsMap.get("receiver") != null && !((String) variablesAsMap.get("receiver")).isEmpty()) {
            if (ValidateEmail.validateEmail((String) variablesAsMap.get("receiver"))) {
                receiver = variablesAsMap.get("receiver").toString();
            }
        }

        try {
            sendMail(sender, receiver, subject, body, filePathArray);

            HashMap<String, Object> variables = new HashMap<>();
            variables.put("result", "The mail was sent with success to " + receiver);

            client.newCompleteCommand(job.getKey())
                    .variables(variables)
                    .send()
                    .exceptionally(throwable -> {
                        throw new RuntimeException("There was a mistake and the job could not be completed", throwable);
                    });

        } catch (MessagingException e) {
            e.printStackTrace();
            client.newFailCommand(job.getKey());
        }
    }

    private void sendMail(String sender, String receiver, String subject, String body, String[] attachmentFilePath) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(sender);
        helper.setTo(receiver);
        helper.setSubject(subject);
        helper.setText(body, true);

        for (String filePath : attachmentFilePath) {
            FileSystemResource file = new FileSystemResource(new File(filePath));
            helper.addAttachment(file.getFilename(), file);
        }

        javaMailSender.send(message);
    }
}
