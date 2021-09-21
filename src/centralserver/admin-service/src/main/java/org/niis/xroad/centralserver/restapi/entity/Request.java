/**
 * The MIT License
 *
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.centralserver.restapi.entity;
// Generated Feb 16, 2021 11:14:33 AM by Hibernate Tools 5.4.20.Final

import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.SecurityServerId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Requests generated by hbm2java
 */
@Entity
@Table(name = Request.TABLE_NAME)
public class Request extends AuditableEntity {
    static final String TABLE_NAME = "requests";

    private int id;
    private SecurityServerId securityServerId;
    private ClientId clientId;
    private RequestProcessing requestProcessing;
    private String type;
    private byte[] authCert;
    private String address;
    private String origin;
    private String serverOwnerName;
    private String serverUserName;
    private String comments;
    private String serverOwnerClass;
    private String serverOwnerCode;
    private String serverCode;
    private String processingStatus;

    public Request() {
        //JPA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = TABLE_NAME + "_id_seq")
    @SequenceGenerator(name = TABLE_NAME + "_id_seq", sequenceName = TABLE_NAME + "_id_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_server_id")
    public SecurityServerId getSecurityServerId() {
        return this.securityServerId;
    }

    public void setSecurityServerId(SecurityServerId securityServerId) {
        this.securityServerId = securityServerId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sec_serv_user_id")
    public ClientId getClientId() {
        return this.clientId;
    }

    public void setClientId(ClientId clientId) {
        this.clientId = clientId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_processing_id")
    public RequestProcessing getRequestProcessing() {
        return this.requestProcessing;
    }

    public void setRequestProcessing(RequestProcessing requestProcessing) {
        this.requestProcessing = requestProcessing;
    }

    @Column(name = "type")
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "auth_cert")
    public byte[] getAuthCert() {
        return this.authCert;
    }

    public void setAuthCert(byte[] authCert) {
        this.authCert = authCert;
    }

    @Column(name = "address")
    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Column(name = "origin")
    public String getOrigin() {
        return this.origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    @Column(name = "server_owner_name")
    public String getServerOwnerName() {
        return this.serverOwnerName;
    }

    public void setServerOwnerName(String serverOwnerName) {
        this.serverOwnerName = serverOwnerName;
    }

    @Column(name = "server_user_name")
    public String getServerUserName() {
        return this.serverUserName;
    }

    public void setServerUserName(String serverUserName) {
        this.serverUserName = serverUserName;
    }

    @Column(name = "comments")
    public String getComments() {
        return this.comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Column(name = "server_owner_class")
    public String getServerOwnerClass() {
        return this.serverOwnerClass;
    }

    public void setServerOwnerClass(String serverOwnerClass) {
        this.serverOwnerClass = serverOwnerClass;
    }

    @Column(name = "server_owner_code")
    public String getServerOwnerCode() {
        return this.serverOwnerCode;
    }

    public void setServerOwnerCode(String serverOwnerCode) {
        this.serverOwnerCode = serverOwnerCode;
    }

    @Column(name = "server_code")
    public String getServerCode() {
        return this.serverCode;
    }

    public void setServerCode(String serverCode) {
        this.serverCode = serverCode;
    }

    @Column(name = "processing_status")
    public String getProcessingStatus() {
        return this.processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

}

