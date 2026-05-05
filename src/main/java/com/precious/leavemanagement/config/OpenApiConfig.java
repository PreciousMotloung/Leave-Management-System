package com.precious.leavemanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Leave Management Service API",
                version = "1.0.0",
                description = """
                        REST API for managing employee leave requests, balances, and approvals.
                        
                        **Features:**
                        - JWT-based authentication
                        - Role-based access control (EMPLOYEE, MANAGER)
                        - Leave request submission with balance validation
                        - Manager approval/rejection workflow
                        - Automatic leave balance management
                        - Overlapping request detection
                        
                        **Authentication:**
                        1. Register or login via /api/auth endpoints
                        2. Copy the JWT token from the response
                        3. Click 'Authorize' button (top right)
                        4. Enter: Bearer {your-token}
                        5. Click 'Authorize' then 'Close'
                        6. All protected endpoints will now include the token
                        
                        **Roles:**
                        - **EMPLOYEE**: Can submit, view own requests, view own balance, cancel own requests
                        - **MANAGER**: All employee permissions + view all pending requests, approve/reject any request, view any user's balance
                        
                        **Leave Types:**
                        - Annual Leave: 21 days
                        - Sick Leave: 10 days
                        - Family Responsibility: 5 days
                        """,
                contact = @Contact(
                        name = "Leave Management Support",
                        email = "support@leavemanagement.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development Server"),
                @Server(url = "https://api.leavemanagement.com", description = "Production Server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT authentication. Obtain token from /api/auth/login or /api/auth/register endpoints. Format: Bearer {token}",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
