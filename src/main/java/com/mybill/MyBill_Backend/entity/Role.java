package com.mybill.MyBill_Backend.entity;

/**
 * Application roles.
 *
 * <ul>
 *   <li>{@code OWNER} – business owner; the primary user of the Owner Portal.
 *       Carries {@code ROLE_USER} Spring authority. Previously stored as {@code CLIENT}
 *       in the database (renamed by Flyway migration V36).</li>
 *   <li>{@code CLIENT} – reserved for the future Client Portal (v2). Not yet used
 *       for authentication.</li>
 *   <li>{@code ADMIN} – system administrator.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    OWNER,
    CLIENT
}
