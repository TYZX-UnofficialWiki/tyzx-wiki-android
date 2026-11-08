package org.tyzx.wiki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.tyzx.wiki.net.Wiki

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onDone: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var okMsg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(50)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = cs.onSurface, modifier = Modifier.size(22.dp))
            }
        }

        Column(Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (register) "注册账号" else "登录",
                style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp),
                color = cs.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "账号用于编辑条目、上传图片和在讨论页发言",
                fontSize = 14.sp,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            Field(value = name, onChange = { name = it }, label = "用户名", enabled = !busy)
            Spacer(Modifier.height(12.dp))
            Field(
                value = pass,
                onChange = { pass = it },
                label = "密码",
                enabled = !busy,
                password = true,
            )
            if (register) {
                Spacer(Modifier.height(12.dp))
                Field(
                    value = email,
                    onChange = { email = it },
                    label = "邮箱（选填）",
                    enabled = !busy,
                    keyboard = KeyboardType.Email,
                )
            }

            if (msg != null) {
                Spacer(Modifier.height(14.dp))
                Text(msg!!, fontSize = 13.sp, color = cs.error)
            }
            if (okMsg != null) {
                Spacer(Modifier.height(14.dp))
                Text(okMsg!!, fontSize = 13.sp, color = cs.primary)
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    if (busy) return@Button
                    if (name.isBlank() || pass.isBlank()) {
                        msg = "用户名和密码都要填"
                        return@Button
                    }
                    busy = true
                    msg = null
                    scope.launch {
                        if (register) {
                            val r = Wiki.register(name.trim(), pass, email.trim())
                            busy = false
                            if (r.ok) {
                                okMsg = "注册成功，已登录"
                                onDone(Wiki.user ?: name.trim())
                            } else {
                                msg = r.error
                                if (r.captcha) {
                                    msg = "站点要求输入验证码。请先在浏览器里注册一次，再用这里的“登录”。"
                                }
                            }
                        } else {
                            val err = Wiki.login(name.trim(), pass)
                            busy = false
                            if (err == null) onDone(Wiki.user ?: name.trim()) else msg = err
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = cs.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text(if (register) "注册" else "登录", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = {
                    register = !register
                    msg = null
                    okMsg = null
                }) {
                    Text(if (register) "已经有账号了？登录" else "还没有账号？注册", fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "这是江苏省天一中学非官方维基客户端，与学校官方无关。",
                fontSize = 12.sp,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(140.dp))
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else keyboard,
        ),
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cs.surfaceContainerHigh,
            unfocusedContainerColor = cs.surfaceContainerHigh,
            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
    )
}
