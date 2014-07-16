package ch.rasc.dataformats;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.msgpack.annotation.Message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement
@Message
public class Address {

	private final static DateTimeFormatter dobFormatter = DateTimeFormatter
			.ofPattern("MM/dd/yyyy");

	@XmlElement
	private int id;

	@XmlElement
	private String lastName;

	@XmlElement
	private String firstName;

	@XmlElement
	private String street;

	@XmlElement
	private String zip;

	@XmlElement
	private String city;

	@XmlElement
	private String country;

	@XmlElement
	private float lat;

	@XmlElement
	private float lng;

	@XmlElement
	private String email;

	@XmlElement
	@JsonSerialize(using = LocalDateSerializer.class)
	private LocalDate dob;

	public Object[] toArray() {
		return new Object[] { id, lastName, firstName, street, zip, city, country, lat,
				lng, email, dob.toString() };
	}

	public Address() {
	}

	public Address(String line) {
		String[] tokens = line.split(";");
		id = Integer.parseInt(tokens[0]);
		lastName = tokens[1];
		firstName = tokens[2];
		street = tokens[3];
		zip = tokens[4];
		city = tokens[5];
		country = tokens[6];

		String[] latlng = tokens[7].split(",");
		lat = Float.parseFloat(latlng[0].trim());
		lng = Float.parseFloat(latlng[1].trim());

		email = tokens[8];
		dob = LocalDate.parse(tokens[9], dobFormatter);
	}

	public int getId() {
		return id;
	}

	public String getLastName() {
		return lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getStreet() {
		return street;
	}

	public String getZip() {
		return zip;
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return country;
	}

	public float getLat() {
		return lat;
	}

	public float getLng() {
		return lng;
	}

	public String getEmail() {
		return email;
	}

	public LocalDate getDob() {
		return dob;
	}

}
