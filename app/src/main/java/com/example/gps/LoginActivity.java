//로그인 화면
package com.example.gps;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;
import com.example.gps.model.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etId, etPw;
    private Button btnLogin, btnSignup, btnFindId, btnFindPw, btnGuest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // XML에서 뷰 연결
        etId = findViewById(R.id.et_id);
        etPw = findViewById(R.id.et_pw);
        btnLogin = findViewById(R.id.btn_login);
        btnSignup = findViewById(R.id.btn_signup);
        btnFindId = findViewById(R.id.btn_find_id);
        btnFindPw = findViewById(R.id.btn_find_pw);

        // 로그인 버튼 클릭
        //btnLogin.setOnClickListener(view -> login());

        // 회원가입 화면으로 이동
        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // 아이디 찾기 화면 이동
        btnFindId.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FindIdActivity.class);
            startActivity(intent);
        });

        // 비밀번호 찾기 화면 이동
        btnFindPw.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FindPwActivity.class);
            startActivity(intent);
        });
        // 비회원 모드
        btnGuest = findViewById(R.id.btn_guest);
        btnGuest.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
            startActivity(intent);
        });
    }

    private void login() {
        String id = etId.getText().toString().trim();
        String pw = etPw.getText().toString().trim();

        // ✅ 관리자 계정 체크
        if (id.equals("admin") && pw.equals("1234")) {
            Toast.makeText(this, "관리자 로그인 성공", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, AdminMain.class);
            startActivity(intent);
            finish();

        } else {
            // ✅ 일반 회원 로그인 (서버에 요청)
            User user = new User(id, pw, null, null);  // 이메일/이름은 필요 없으니까 null로

            UserApi userApi = ApiClient.getClient().create(UserApi.class);
            Call<Map<String, String>> call = userApi.login(user);

            call.enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String status = response.body().get("status");
                        String message = response.body().get("message");

                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                        if ("success".equals(status)) {
                            // 일반회원 로그인 성공 시 Main 화면으로 이동
                            Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        // 실패 시는 그냥 토스트로 안내하고 아무것도 안 함

                    } else {
                        Toast.makeText(LoginActivity.this, "서버 응답 실패", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
