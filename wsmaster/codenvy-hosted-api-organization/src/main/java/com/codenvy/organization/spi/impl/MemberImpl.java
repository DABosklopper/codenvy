/*
 * Copyright (c) [2012] - [2017] Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package com.codenvy.organization.spi.impl;

import com.codenvy.organization.api.permissions.OrganizationDomain;
import com.codenvy.organization.shared.model.Member;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.eclipse.che.api.permission.server.model.impl.AbstractPermissions;

/**
 * Data object for {@link Member}.
 *
 * @author Sergii Leschenko
 */
@Entity(name = "Member")
@NamedQueries({
  @NamedQuery(
    name = "Member.getMember",
    query =
        "SELECT m "
            + "FROM Member m "
            + "WHERE m.userId = :userId AND m.organizationId = :organizationId"
  ),
  @NamedQuery(
    name = "Member.getByOrganization",
    query = "SELECT m " + "FROM Member m " + "WHERE m.organizationId = :organizationId"
  ),
  @NamedQuery(
    name = "Member.getCountByOrganizationId",
    query = "SELECT COUNT(m) " + "FROM Member m " + "WHERE m.organizationId = :organizationId"
  ),
  @NamedQuery(
    name = "Member.getByUser",
    query = "SELECT m " + "FROM Member m " + "WHERE m.userId = :userId"
  ),
  @NamedQuery(
    name = "Member.getOrganizations",
    query = "SELECT org " + "FROM Member m, m.organization org " + "WHERE m.userId = :userId"
  ),
  @NamedQuery(
    name = "Member.getOrganizationsCount",
    query = "SELECT COUNT(m) " + "FROM Member m " + "WHERE m.userId = :userId "
  )
})
@Table(name = "member")
public class MemberImpl extends AbstractPermissions implements Member {
  @Column(name = "organizationid")
  private String organizationId;

  @ElementCollection(fetch = FetchType.EAGER)
  @Column(name = "actions")
  protected List<String> actions;

  @ManyToOne
  @JoinColumn(
    name = "organizationid",
    referencedColumnName = "id",
    insertable = false,
    updatable = false
  )
  private OrganizationImpl organization;

  public MemberImpl() {}

  public MemberImpl(String userId, String organizationId, List<String> actions) {
    super(userId);
    this.organizationId = organizationId;
    if (actions != null) {
      this.actions = actions;
    }
  }

  public MemberImpl(Member member) {
    this(member.getUserId(), member.getOrganizationId(), member.getActions());
  }

  @Override
  public String getInstanceId() {
    return organizationId;
  }

  @Override
  public String getDomainId() {
    return OrganizationDomain.DOMAIN_ID;
  }

  @Override
  public List<String> getActions() {
    return actions;
  }

  @Override
  public String getOrganizationId() {
    return organizationId;
  }

  @Override
  public String toString() {
    return "MemberImpl{"
        + "userId='"
        + userId
        + '\''
        + ", organizationId='"
        + organizationId
        + '\''
        + ", actions="
        + actions
        + '}';
  }
}
