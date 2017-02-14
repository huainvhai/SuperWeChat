/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ucai.superwechat.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;

import cn.ucai.superwechat.SuperWeChatHelper;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.domain.Result;
import cn.ucai.superwechat.net.NetDao;
import cn.ucai.superwechat.net.OnCompleteListener;
import cn.ucai.superwechat.utils.MFGT;
import cn.ucai.superwechat.utils.ResultUtils;

import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.widget.EaseAlertDialog;

public class AddContactActivity extends BaseActivity {
    private static final String TAG = AddContactActivity.class.getSimpleName();
    private EditText editText;
    private RelativeLayout searchedUserLayout;
    private Button searchBtn;
    private String toAddUsername;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_add_contact);
        TextView mTextView = (TextView) findViewById(R.id.add_list_friends);

        editText = (EditText) findViewById(R.id.edit_note);
        String strAdd = getResources().getString(R.string.add_friend);
        mTextView.setText(strAdd);
        String strUserName = getResources().getString(R.string.user_name);
        editText.setHint(strUserName);
        searchedUserLayout = (RelativeLayout) findViewById(R.id.ll_user);
        searchBtn = (Button) findViewById(R.id.search);
    }


    /**
     * search contact
     *
     * @param v
     */
    public void searchContact(View v) {
        final String name = editText.getText().toString();
        String saveText = searchBtn.getText().toString();

        if (getString(R.string.button_search).equals(saveText)) {
            toAddUsername = name;
            if (TextUtils.isEmpty(name)) {
                new EaseAlertDialog(this, R.string.Please_enter_a_username).show();
                return;
            }
            if(name.equals(EMClient.getInstance().getCurrentUser())){
                new EaseAlertDialog(this, R.string.not_add_myself).show();
                return;
            }

            progressDialog = new ProgressDialog(this);
            String str = getResources().getString(R.string.addcontact_search);
            progressDialog.setMessage(str);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            searchedUserLayout.setVisibility(View.GONE);
            searchAppUser(name);
            // TODO you can search the user from your app server here.

            //show the userame and add button if user exist
        }
    }

    private void searchAppUser(String name) {
        NetDao.findUserByUserName(this, name, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                progressDialog.dismiss();
                boolean isSuccess = false;
                if (s != null) {
                    Result result = ResultUtils.getResultFromJson(s, User.class);
                    if (result != null) {
                        User user = (User) result.getRetData();
                        Log.e(TAG, "user=" + user);
                        if (user != null) {
                            isSuccess = true;
                            MFGT.gotoFriend(AddContactActivity.this, user);
                        }
                    }
                }
                if(!isSuccess){
                    searchedUserLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                Log.e(TAG, "error=" + error);
            }
        });
    }


    public void back(View v) {
        finish();
    }
}
