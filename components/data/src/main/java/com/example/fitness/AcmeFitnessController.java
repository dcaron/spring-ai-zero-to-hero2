package com.example.fitness;

import com.example.model.Customer;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simulated customer login for the ACME fitness demo. Used by Stage 7 demo 02 (model-directed loop)
 * to give the agent a "user context" to reason about.
 */
@RestController
@RequestMapping("/acme")
public class AcmeFitnessController {

  private final CustomerService customerService;

  public AcmeFitnessController(CustomerService customerService) {
    this.customerService = customerService;
  }

  /**
   * Look up a customer by email. Accepts {@code {"email": "…"}} as JSON body. Returns the full
   * {@link Customer} record when found, HTTP 401 when the email is not in the seed data.
   */
  @PostMapping("/login")
  public Customer login(@RequestBody Map<String, String> body) {
    String email = body == null ? null : body.get("email");
    if (email == null || email.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
    }
    Optional<Customer> customer = customerService.findCustomerByEmail(email);
    return customer.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }
}
