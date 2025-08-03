package examples.io.github.jchejarla.springbatch.clustering.advancedjob;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerProcessor implements ItemProcessor<Customer, Customer> {

    @Override
    public Customer process(Customer customer) {
        // Example transformation: Add a domain label to the email
        if (customer.getEmail() != null && !customer.getEmail().contains("+tag")) {
            String[] parts = customer.getEmail().split("@");
            customer.setEmail(parts[0] + "+tag@" + parts[1]);
        }
        return customer;
    }
}
