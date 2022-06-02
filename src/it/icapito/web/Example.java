package it.icapito.web;

import it.icapito.web.RestClient.HttpMethod;
import it.icapito.web.RestClient.HttpRequest;

public class Example {

	public static void main(String[] args) {
		testPost();
		testExpiredCert();
		testPut();
	}
	
	public static void testExpiredCert() {
		HttpRequest request = RestClient.createRequest();
		request.setUrl("https://expired.badssl.com/");
		if (request.disableCertificateValidation())
			if (request.execute())
				System.out.println("Response disabled certificate check: \n" + request.getResponse().getBody());
		request.enableCertificateValidation();
		if (request.execute())
			System.out.println("Response enabled certificate check: \n" + request.getResponse().getBody());
		else
			if (request.isError())
				System.out.println("Request error message: \n" + request.getErrorMessage());
	}
	
	public static void testPost() {
		doRequestWithBody(HttpMethod.POST);
	}
	
	public static void testPut() {
		doRequestWithBody(HttpMethod.PUT);
	}
	
	
	private static void doRequestWithBody(HttpMethod method) {
		HttpRequest request = RestClient.createRequest();
		request.setMethod(method);
		request.setUrl("https://postman-echo.com/" + method.name().toLowerCase());
		String requestBody = "{\"StringKey\":\"This is a string example\",\"booleanKey\":false,"
				+ "\"favs\":[\"https://icapito.it\",\"https://www.icapito.it\",\"https://www.underatrain.it\"],"
				+ "\"user\":{\"FirstName\":\"Black\",\"LastName\":\"Codec\"}}";
		System.out.println("Request body: \n" + requestBody);
		request.setContentType("application/json");
		request.setBody(requestBody);
		if (request.execute())
			System.out.println("Response body: \n" + request.getResponse().getBody());
		else 
			if (request.isError())
				System.out.println("Request error: \n" + request.getErrorMessage());
	}
}
