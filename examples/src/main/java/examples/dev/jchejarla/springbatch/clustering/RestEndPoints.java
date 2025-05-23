package examples.dev.jchejarla.springbatch.clustering;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Random;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class RestEndPoints {

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;

    @GetMapping("/startjob")
    public String startJob() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("RUN_TIME", LocalDateTime.now().toString(), true).toJobParameters();
        Job job = applicationContext.getBean("clusteredJob", Job.class);
        try {
            jobLauncher.run(job, parameters);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(new Random().nextInt());
    }


    @GetMapping("/hostname")
    public String getHostname() throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        return String.format(
                "Hostname: %s\nHostAddress: %s\nCanonicalHostname: %s",
                inetAddress.getHostName(),
                inetAddress.getHostAddress(),
                inetAddress.getCanonicalHostName()
        );
    }

}
