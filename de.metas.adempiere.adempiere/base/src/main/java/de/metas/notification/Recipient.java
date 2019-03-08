package de.metas.notification;

import org.adempiere.exceptions.AdempiereException;

import de.metas.user.UserId;
import de.metas.util.Check;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Value
@EqualsAndHashCode(doNotUseGetters = true)
@ToString(doNotUseGetters = true)
public class Recipient
{
	public static Recipient allUsers()
	{
		return ALL_USERS;
	}

	public static Recipient user(final int userId)
	{
		return _builder()
				.type(RecipientType.User)
				.userId(userId)
				.build();
	}
	
	public static Recipient user(@NonNull final UserId userId)
	{
		return _builder()
				.type(RecipientType.User)
				.userId(userId.getRepoId())
				.build();
	}


	public static Recipient userAndRole(final int userId, final int roleId)
	{
		return _builder()
				.type(RecipientType.User)
				.userId(userId)
				.roleId(roleId)
				.build();
	}

	public static Recipient role(final int roleId)
	{
		return _builder()
				.type(RecipientType.Role)
				.roleId(roleId)
				.build();
	}

	public static Recipient allRolesContainingGroup(final NotificationGroupName notificationGroupName)
	{
		return _builder()
				.type(RecipientType.AllRolesContainingGroup)
				.notificationGroupName(notificationGroupName)
				.build();
	}

	public static enum RecipientType
	{
		AllUsers, User, Role, AllRolesContainingGroup,
	}

	private static final Recipient ALL_USERS = _builder().type(RecipientType.AllUsers).build();

	private RecipientType type;
	private int userId;
	private int roleId;
	private NotificationGroupName notificationGroupName;

	@Builder(builderMethodName = "_builder")
	private Recipient(
			@NonNull final RecipientType type,
			final int userId,
			final int roleId,
			final NotificationGroupName notificationGroupName)
	{
		this.type = type;
		if (type == RecipientType.AllUsers)
		{
			this.userId = -1;
			this.roleId = -1;
			this.notificationGroupName = null;
		}
		else if (type == RecipientType.User)
		{
			Check.assumeGreaterOrEqualToZero(userId, "userId");
			this.userId = userId;
			this.roleId = roleId >= 0 ? roleId : -1;
			this.notificationGroupName = null;
		}
		else if (type == RecipientType.Role)
		{
			Check.assumeGreaterOrEqualToZero(roleId, "roleId");
			this.userId = -1;
			this.roleId = roleId;
			this.notificationGroupName = null;
		}
		else if (type == RecipientType.AllRolesContainingGroup)
		{
			Check.assumeNotNull(notificationGroupName, "Parameter notificationGroupName is not null");
			this.userId = -1;
			this.roleId = -1;
			this.notificationGroupName = notificationGroupName;
		}
		else
		{
			throw new IllegalArgumentException("Unknown: " + type);
		}
	}

	public boolean isAllUsers()
	{
		return type == RecipientType.AllUsers;
	}

	public boolean isUser()
	{
		return type == RecipientType.User;
	}

	public boolean isRole()
	{
		return type == RecipientType.Role;
	}

	public boolean isAllRolesContainingGroup()
	{
		return type == RecipientType.AllRolesContainingGroup;
	}

	public int getUserId()
	{
		if (type != RecipientType.User)
		{
			throw new AdempiereException("UserId not available: " + this);
		}
		return userId;
	}

	public boolean isRoleIdSet()
	{
		return roleId >= 0;
	}

	public NotificationGroupName getNotificationGroupName()
	{
		if (type != RecipientType.AllRolesContainingGroup)
		{
			throw new AdempiereException("NotificationGroupName not available: " + this);
		}
		return notificationGroupName;
	}

}
