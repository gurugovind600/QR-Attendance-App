package com.attendanceapp.myapplication.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendanceapp.myapplication.model.Student

@Composable
fun ProfileScreen(
    student: Student,
    email: String,
    onSaveProfile: (String, String) -> Unit // address, category
) {
    // Local state for editing
    var address by remember(student) { mutableStateOf(student.address) } // You need to add 'address' to Student model
    var category by remember(student) { mutableStateOf(student.category) } // Add 'category' to Student model
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Top Header with Avatar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF3D5AFE)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(10.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(student.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Roll: ${student.rollNo}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }

        // 2. Details Section
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Personal Details", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Read-Only Fields
            ProfileField(label = "Full Name", value = student.name, isReadOnly = true)
            ProfileField(label = "Roll Number", value = student.rollNo, isReadOnly = true)
            ProfileField(label = "Email Address", value = email, isReadOnly = true)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Editable Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Editable Info", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    if (isEditing) onSaveProfile(address, category)
                    isEditing = !isEditing
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Star else Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF3D5AFE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ProfileEditField(
                label = "Address",
                value = address,
                onValueChange = { address = it },
                isEditable = isEditing
            )

            ProfileEditField(
                label = "Category",
                value = category,
                onValueChange = { category = it },
                isEditable = isEditing
            )

            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onSaveProfile(address, category)
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}
@Composable
fun ProfileField(label: String, value: String, isReadOnly: Boolean) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileEditField(label: String, value: String, onValueChange: (String) -> Unit, isEditable: Boolean) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = Color.White,
                disabledTextColor = Color.Black,
                disabledBorderColor = Color.Transparent
            )
        )
    }
}
