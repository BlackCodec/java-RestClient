package it.icapito.web;

import it.icapito.web.RestClient.HttpRequestMethod;
import it.icapito.web.RestClient.HttpRequest;

public class Example {

	public static void main(String[] args) {
		testPost();
		testPatch();
		testPut();
		testExpiredCert();
	}
	
	private static void testExpiredCert() {
		print(null, "testExpriredCert");
		HttpRequest request = RestClient.createRequest();
		request.setUrl("https://expired.badssl.com/");
		if (request.disableCertificateValidation() && request.execute())
				print("Response disabled certificate check", request.getResponse().getBody());
		if (request.enableCertificateValidation() && request.execute())
			print("Response enabled certificate check",request.getResponse().getBody());
		else if (request.isError())
			print("Request error message",request.getErrorMessage());
	}
	
	private static void testPost() { print(null, "testPost"); doRequestWithBody(HttpRequestMethod.POST); }
	private static void testPut() { print(null, "testPut");doRequestWithBody(HttpRequestMethod.PUT); }
	private static void testPatch() { print(null, "testPatch"); doRequestWithBody(HttpRequestMethod.PATCH); }
	
	private static void print(String title, String msg) {
		String titleStr = (title != null)?String.format("%s:%n", title):"";
		String msgStr = (title != null)?String.format("\t%s%n", msg):String.format("%s", msg);
		System.out.println(String.format("<%s> %s%s", 
				(new java.util.Date()).toString(), titleStr, msgStr)); 
	}
	
	private static void doRequestWithBody(HttpRequestMethod method) {
		HttpRequest request = RestClient.createRequest();
		request.setMethod(method);
		request.setUrl("https://postman-echo.com/" + method.name().toLowerCase());
		String requestBody = "{\"StringKey\":\"This is a string example\",\"booleanKey\":false,"
				+ "\"favs\":[\"https://icapito.it\",\"https://www.icapito.it\",\"https://www.underatrain.it\"],"
				+ "\"user\":{\"FirstName\":\"Black\",\"LastName\":\"Codec\"}}";
		print("Request body",requestBody);
		request.setContentType("application/json");
		request.setBody(requestBody);
		if (request.execute())
			print("Response body",request.getResponse().getBody());
		else 
			if (request.isError())
				print("Request error", request.getErrorMessage());
	}
}
