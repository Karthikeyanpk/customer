/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (3.0.0-SNAPSHOT).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.diviso.graeshoppe.customer.client.SMS;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.diviso.graeshoppe.customer.domain.OTPChallenge;
import com.diviso.graeshoppe.customer.domain.OTPResponse;


public interface SMSResourceApiUK {

    @PostMapping(value = "/otp_send")
	OTPResponse sendSMS(@RequestParam(value="message") String message, @RequestParam(value="apiKey") String apiKey, @RequestParam(value="numbers") long  numbers, @RequestParam(value="sender") String sender);
    
    @PostMapping(value = "/otp_challenge")
	OTPChallenge verifyOTP(@RequestParam(value="numbers") long numbers, @RequestParam(value="code") String code, @RequestParam(value="apiKey") String apiKey);

}
