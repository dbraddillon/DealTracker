package dev.basementlab.dealtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication = @Configuration + @ComponentScan + @EnableAutoConfiguration bundled -
// roughly the combination of Program.cs's builder.Services wiring + implicit assembly scanning
// in an ASP.NET Core minimal API, except component scanning here is *convention-based* (scans
// this class's package and subpackages) rather than explicit registration.
@SpringBootApplication
@EnableScheduling // required for @Scheduled to do anything - like AddHostedService<T> in .NET, opt-in not automatic
public class DealTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealTrackerApplication.class, args);
    }
}
