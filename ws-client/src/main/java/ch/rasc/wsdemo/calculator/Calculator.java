package ch.rasc.wsdemo.calculator;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.1.6 in JDK 6 Generated
 * source version: 2.1
 * 
 */
@WebService(name = "Calculator", targetNamespace = "http://wsdemo.ralscha.ch/")
@XmlSeeAlso({ ObjectFactory.class })
public interface Calculator {

	/**
	 * 
	 * @param arg1
	 * @param arg0
	 * @return returns int
	 */
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(localName = "add", targetNamespace = "http://wsdemo.ralscha.ch/", className = "ch.rasc.wsdemo.calculator.Add")
	@ResponseWrapper(localName = "addResponse", targetNamespace = "http://wsdemo.ralscha.ch/", className = "ch.rasc.wsdemo.calculator.AddResponse")
	public int add(@WebParam(name = "arg0", targetNamespace = "") int arg0, @WebParam(name = "arg1", targetNamespace = "") int arg1);

	/**
	 * 
	 * @param arg1
	 * @param arg0
	 * @return returns int
	 */
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(localName = "multiply", targetNamespace = "http://wsdemo.ralscha.ch/", className = "ch.rasc.wsdemo.calculator.Multiply")
	@ResponseWrapper(localName = "multiplyResponse", targetNamespace = "http://wsdemo.ralscha.ch/", className = "ch.rasc.wsdemo.calculator.MultiplyResponse")
	public int multiply(@WebParam(name = "arg0", targetNamespace = "") int arg0, @WebParam(name = "arg1", targetNamespace = "") int arg1);

	/**
	 * 
	 * @param arg1
	 * @param arg0
	 * @return returns int
	 */
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(localName = "divide", targetNamespace = "http://wsdemo.ralscha.ch/", className = "ch.rasc.wsdemo.calculator.Divide")
	@ResponseWrapper(localName = "divideResponse", targetNamespace = "http://wsdemo.ralscha.ch/", className = "ch.rasc.wsdemo.calculator.DivideResponse")
	public int divide(@WebParam(name = "arg0", targetNamespace = "") int arg0, @WebParam(name = "arg1", targetNamespace = "") int arg1);

	/**
	 * 
	 * @param arg1
	 * @param arg0
	 * @return returns int
	 */
	@WebMethod
	@WebResult(targetNamespace = "")
	@RequestWrapper(localName = "subtract", targetNamespace = "http://wsdemo.ralscha.ch/", className = "ch.rasc.wsdemo.calculator.Subtract")
	@ResponseWrapper(localName = "subtractResponse", targetNamespace = "http://wsdemo.ralscha.ch/", className = "ch.rasc.wsdemo.calculator.SubtractResponse")
	public int subtract(@WebParam(name = "arg0", targetNamespace = "") int arg0, @WebParam(name = "arg1", targetNamespace = "") int arg1);

}
