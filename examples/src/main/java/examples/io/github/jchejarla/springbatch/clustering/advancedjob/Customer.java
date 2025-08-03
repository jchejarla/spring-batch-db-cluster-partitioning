package examples.io.github.jchejarla.springbatch.clustering.advancedjob;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Customer {
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String signupDate;
}

