/**
 * Copyright (c) 2012-2013, JCabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.github.mock;

import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.github.Github;
import com.jcabi.github.Organization;
import com.jcabi.github.Organizations;
import com.jcabi.github.User;
import com.jcabi.xml.XML;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.xembly.Directives;

/**
 * Github organizations.
 * @author Paul Polishchuk (ppol@ua.fm)
 * @version $Id$
 * @see <a href="http://developer.github.com/v3/orgs/">Organizations API</a>
 * @since 0.7
 * @checkstyle MultipleStringLiteralsCheck (200 lines)
 * @checkstyle ClassDataAbstractionCoupling (200 lines)
 */
@Immutable
@Loggable(Loggable.DEBUG)
@ToString
@EqualsAndHashCode(of = { "storage", "self" })
final class MkOrganizations implements Organizations {

    /**
     * Random generator.
     */
    private static final Random RAND = new SecureRandom();

    /**
     * Storage.
     */
    private final transient MkStorage storage;

    /**
     * Login of the user logged in.
     */
    private final transient String self;

    /**
     * Public ctor.
     * @param stg Storage
     * @param login User to login
     * @throws IOException If there is any I/O problem
     */
    MkOrganizations(final MkStorage stg, final String login)
        throws IOException {
        this.storage = stg;
        this.self = login;
        this.storage.apply(
            new Directives().xpath("/github").addIf("orgs")
        );
    }

    @Override
    public Github github() {
        return new MkGithub(this.storage, this.self);
    }

    @Override
    public User user() {
        try {
            return new MkUser(this.storage, this.self);
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public Organization get(final String login) {
        try {
            this.storage.apply(
                new Directives().xpath(this.xpath())
                    .add("org")
                    .add("id")
                    .set(Integer.toString(RAND.nextInt()))
            );
            this.storage.apply(
                new Directives().xpath(
                    String.format("/github/orgs[not(org[login='%s'])]", login)
                ).add("org").add("login").set(login)
            );
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
        // @checkstyle AnonInnerLength (50 lines)
        return new Organization() {
            @Override
            public Github github() {
                return new MkGithub(MkOrganizations.this.storage, login);
            }
            @Override
            public String login() {
                return login;
            }
            @Override
            public JsonObject json() {
                return Json.createObjectBuilder()
                    .add("name", "github")
                    .add("company", "GitHub")
                    .add("blog", "https://github.com/blog")
                    .add("location", "San Francisco")
                    .add("email", "octocat@github.com")
                    .add("public_repos", MkOrganizations.RAND.nextInt())
                    .add("public_gists", MkOrganizations.RAND.nextInt())
                    .add("followers", MkOrganizations.RAND.nextInt())
                    .add("following", MkOrganizations.RAND.nextInt())
                    .add("html_url", "https://github.com/octocat")
                    .add("created_at", "2008-01-14T04:33:35Z")
                    .add("type", "Organization")
                    .build();
            }
            @Override
            public int compareTo(final Organization obj) {
                return this.login().compareTo(obj.login());
            }
            @Override
            public void patch(final JsonObject json) throws IOException {
                new JsonPatch(MkOrganizations.this.storage)
                    .patch(MkOrganizations.this.xpath(), json);
            }
        };
    }

    @Override
    public Iterable<Organization> iterate() {
        return new MkIterable<Organization>(
            this.storage,
            String.format("%s/org", this.xpath()),
            new MkIterable.Mapping<Organization>() {
                @Override
                public Organization map(final XML xml) {
                    return MkOrganizations.this.get(
                        xml.xpath("login/text()").get(0)
                    );
                }
            }
        );
    }

    /**
     * XPath of this element in XML tree.
     * @return XPath
     */
    private String xpath() {
        return "/github/orgs";
    }
}
