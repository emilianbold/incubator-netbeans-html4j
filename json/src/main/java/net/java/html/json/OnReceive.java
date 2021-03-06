/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.java.html.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Static methods in classes annotated by {@link Model}
 * can be marked by this annotation to establish a
 * <a href="http://en.wikipedia.org/wiki/JSON">JSON</a>
 * communication point. The first argument should be the
 * associated {@link Model} class. The second argument can
 * be another class generated by {@link Model} annotation,
 * or array of such classes or (since 0.8.1 version) a
 * {@link java.util.List} of such classes.
 * The associated model class then gets new method to invoke a network
 * connection asynchronously. Example follows:
 *
 * <pre>
 * {@link Model @Model}(className="MyModel", properties={
 *   {@link Property @Property}(name = "people", type=Person.class, array=true)
 * })
 * class MyModelImpl {
 *   {@link Model @Model}(className="Person", properties={
 *     {@link Property @Property}(name = "firstName", type=String.class),
 *     {@link Property @Property}(name = "lastName", type=String.class)
 *   })
 *   static class PersonImpl {
 *     {@link ComputedProperty @ComputedProperty}
 *     static String fullName(String firstName, String lastName) {
 *       return firstName + " " + lastName;
 *     }
 *   }
 *
 *   {@link OnReceive @OnReceive}(url = "{protocol}://your.server.com/person/{name}")
 *   static void getANewPerson(MyModel m, Person p) {
 *     System.out.println("Adding " + p.getFullName() + '!');
 *     m.getPeople().add(p);
 *   }
 *
 *   // the above will generate method <code>getANewPerson</code> in class <code>MyModel</code>.
 *   // with <code>protocol</code> and <code>name</code> arguments
 *   // which asynchronously contacts the server and in case of success calls
 *   // your {@link OnReceive @OnReceive} with parsed in data
 *
 *   {@link Function @Function}
 *   static void requestSmith(MyModel m) {
 *     m.getANewPerson("http", "Smith");
 *   }
 * }
 * </pre>
 * When the server returns <code>{ "firstName" : "John", "lastName" : "Smith" }</code>
 * the system will print a message <em>Adding John Smith!</em>. It is not
 * necessary to fully describe the server message - enumerate only the fields
 * in the response you are interested in. The others will be discarded. So,
 * if the server <code>{ "firstName" : "John", "lastName" : "Smith", "age" : 33 }</code>
 * the above code will behave the same (e.g. ignore the <code>age</code>
 * value).
 * <p>
 * One can use this method to communicate with the server
 * via <a href="doc-files/websockets.html">WebSocket</a> protocol since version 0.6.
 * Read the <a href="doc-files/websockets.html">tutorial</a> to see how.
 * The method shall be non-private
 * and unless {@link Model#instance() instance mode} is on also static.
 * <p>
 * Visit an <a target="_blank" href="http://dew.apidesign.org/dew/#7138581">on-line demo</a>
 * to see REST access via {@link OnReceive} annotation.
 *
 * @author Jaroslav Tulach
 * @since 0.3
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OnReceive {
    /** The URL to connect to. Can contain variable names surrounded by '{' and '}'.
     * Those names will then become parameters of the associated method.
     *
     * @return the (possibly parametrized) url to connect to
     */
    String url();

    /** Specifies HTTP request headers. Array of header lines
     * can contain variable names surrounded by '{' and '}'.
     * Those names will then become parameters of the associated method
     * (in addition to those added by {@link #url()} specification)
     * and can only be used with plain JSON(P) requests.
     * Headers are currently <b>not</b> supported by the
     * <a href="doc-files/websockets.html">WebSockets protocol</a>.
     * A sample follows. If you want to transmit <b>X-Birthday</b> header,
     * you can do it like this:
     * <pre>
     * {@code @}{@link OnReceive}(url="http://your.server.org", headers = {
     *   "X-Birthday: {dayOfBirth}"
     * })
     * <b>static void</b> knowingTheBirth({@link Model YourModel} model) {
     *   // handle the reply
     * }</pre>
     * a method <b>knowingTheBirth</b> is generated in
     * <code>YourModel</code> class with the <code>dayOfBirth</code> argument
     * which can be called like this:
     * <pre>
     * yourModel.knowingTheBirth("10. 12. 1973");
     * </pre>
     *
     * @return array of header lines - each line should be plain text with
     *   a header name, followed by ":" and value usually specified as
     *   '{' and '}' surrounded variable. The line shouldn't contain
     *   newline or other control characters
     * @since 1.2
     */
    String[] headers() default {};

    /** Support for <a href="http://en.wikipedia.org/wiki/JSONP">JSONP</a> requires
     * a callback from the server generated page to a function defined in the
     * system. The name of such function is usually specified as a property
     * (of possibly different names). By defining the <code>jsonp</code> attribute
     * one turns on the <a href="http://en.wikipedia.org/wiki/JSONP">JSONP</a>
     * transmission and specifies the name of the property. The property should
     * also be used in the {@link #url()} attribute on appropriate place.
     *
     * @return name of a property to carry the name of <a href="http://en.wikipedia.org/wiki/JSONP">JSONP</a>
     *    callback function.
     */
    String jsonp() default "";

    /** The model class to be send to the server as JSON data.
     * By default no data are sent. However certain {@link #method() transport methods}
     * (like <code>"PUT"</code> and <code>"POST"</code>) require the
     * data to be specified.
     *
     * @return name of a class generated using {@link Model @Model} annotation
     * @since 0.3
     */
    Class<?> data() default Object.class;

    /** The HTTP transfer method to use. Defaults to <code>"GET"</code>.
     * Other typical methods include <code>"HEAD"</code>,
     * <code>"DELETE"</code>, <code>"POST"</code>, <code>"PUT"</code>.
     * The last two mentioned methods require {@link #data()} to be specified.
     * <p>
     * When {@link #jsonp() JSONP} transport is requested, the method
     * has to be <code>"GET"</code>.
     * <p>
     * Since version 0.5 one can specify "<a href="doc-files/websockets.html">WebSocket</a>"
     * as the communication method.
     *
     * @return name of the HTTP transfer method
     * @since 0.3
     */
    String method() default "GET";

    /** Name of a method in this class which should be called in case of
     * an error. The method has to be non-private and take one model and
     * one {@link Exception}
     * parameter. If this method is not specified, the exception is just
     * printed to console.
     *
     * @return name of method in this class
     * @since 0.5
     */
    public String onError() default "";
}
