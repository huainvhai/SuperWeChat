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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMGroupManager.EMGroupOptions;
import com.hyphenate.chat.EMGroupManager.EMGroupStyle;
import com.hyphenate.easeui.domain.Group;
import com.hyphenate.easeui.utils.EaseImageUtils;
import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.exceptions.HyphenateException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.domain.Result;
import cn.ucai.superwechat.net.NetDao;
import cn.ucai.superwechat.net.OnCompleteListener;
import cn.ucai.superwechat.utils.CommonUtils;
import cn.ucai.superwechat.utils.ResultUtils;

public class NewGroupActivity extends BaseActivity {
    private static final String TAG = NewGroupActivity.class.getSimpleName();
    @BindView(R.id.edit_group_name)
    EditText editGroupName;
    @BindView(R.id.edit_group_introduction)
    EditText editGroupIntroduction;
    @BindView(R.id.layout_group_icon)
    LinearLayout layoutGroupIcon;
    @BindView(R.id.cb_public)
    CheckBox cbPublic;
    @BindView(R.id.second_desc)
    TextView secondDesc;
    @BindView(R.id.cb_member_inviter)
    CheckBox cbMemberInviter;
    @BindView(R.id.iv_avatar)
    ImageView ivAvatar;

    private ProgressDialog progressDialog;
    File file = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.em_activity_new_group);
        ButterKnife.bind(this);

        cbPublic.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    secondDesc.setText(R.string.join_need_owner_approval);
                } else {
                    secondDesc.setText(R.string.Open_group_members_invited);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case I.REQUESTCODE_PICK:
                    if (data == null || data.getData() == null) {
                        return;
                    }
                    startPhotoZoom(data.getData());
                    break;
                case I.REQUESTCODE_CUTTING:
                    if (data != null) {
                        saveBitmapFile(data);
                    }
                    break;
                case I.REQUESTCODE_MEMBER:
                    createEMGroup(data);
                    break;
                default:
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void saveBitmapFile(Intent picdata) {
        Bundle extras = picdata.getExtras();
        if (extras != null) {
            Bitmap bitmap = extras.getParcelable("data");
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            ivAvatar.setImageDrawable(drawable);
            String imagePath = EaseImageUtils.getImagePath(EMClient.getInstance().getCurrentUser() + I.AVATAR_SUFFIX_JPG);
            file = new File(imagePath);  //将要保存的图片的路径
            Log.e(TAG, "file path = " + file.getAbsolutePath());
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createEMGroup(final Intent data) {
        String st1 = getResources().getString(R.string.Is_to_create_a_group_chat);
        final String st2 = getResources().getString(R.string.Failed_to_create_groups);
        //new group
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(st1);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String groupName = editGroupName.getText().toString().trim();
                String desc = editGroupIntroduction.getText().toString();
                String[] members = data.getStringArrayExtra("newmembers");
                Log.e(TAG,"members=" + members);
                try {
                    EMGroupOptions option = new EMGroupOptions();
                    option.maxUsers = 200;

                    String reason = NewGroupActivity.this.getString(R.string.invite_join_group);
                    reason = EMClient.getInstance().getCurrentUser() + reason + groupName;

                    if (cbPublic.isChecked()) {
                        option.style = cbMemberInviter.isChecked() ? EMGroupStyle.EMGroupStylePublicJoinNeedApproval : EMGroupStyle.EMGroupStylePublicOpenJoin;
                    } else {
                        option.style = cbMemberInviter.isChecked() ? EMGroupStyle.EMGroupStylePrivateMemberCanInvite : EMGroupStyle.EMGroupStylePrivateOnlyOwnerInvite;
                    }
                    EMGroup group = EMClient.getInstance().groupManager().createGroup(groupName, desc, members, reason, option);
                    createAppGroup(group, members);

                } catch (final HyphenateException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(NewGroupActivity.this, st2 + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }

            }
        }).start();
    }

    private void createAppGroup(final EMGroup group, final String[] members) {
        Log.e(TAG, "file=" + file);
        NetDao.createGroup(this, group, file, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (s != null) {
                    Log.e(TAG, "s=" + s);
                    Result result = ResultUtils.getResultFromJson(s, Group.class);
                    Log.e(TAG, "result=" + result);
                    if (result != null) {
                        if (result.isRetMsg()) {
                            if (members != null && members.length > 0) {
                                Log.e(TAG,"进入添加群组成员界面");
                                addGroupMembers(group.getGroupId(), members);
                            } else {
                                createGroupSuccess();
                            }
                        } else {
                            progressDialog.dismiss();
                            if (result.getRetCode() == I.MSG_GROUP_HXID_EXISTS) {
                                CommonUtils.showShortToast("群组环信ID已经存在");
                            }
                            if (result.getRetCode() == I.MSG_GROUP_CREATE_FAIL) {
                                CommonUtils.showShortToast(R.string.Failed_to_create_groups);
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "error=" + error);
                progressDialog.dismiss();
                CommonUtils.showShortToast(R.string.Failed_to_create_groups);
            }
        });
    }

    private void addGroupMembers(String groupId, final String[] members) {
        Log.e(TAG,"addGroupMembers");
        NetDao.addGroupMember(this, getGroupMembers(members), groupId, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.e(TAG, "addGroupMembers,s=" + s);
                progressDialog.dismiss();
                boolean success = false;
                if (s != null) {
                    Result result = ResultUtils.getResultFromJson(s, Group.class);
                    Log.e(TAG, "addGroupMembers,result=" + result);
                    if (result != null && result.isRetMsg()) {
                        success = true;
                        Log.e(TAG, members.toString());
                        createGroupSuccess();
                    }
                }
                if (!success) {
                    progressDialog.dismiss();
                    CommonUtils.showShortToast(R.string.Failed_to_create_groups);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "error=" + error);
                progressDialog.dismiss();
                CommonUtils.showShortToast(R.string.Failed_to_create_groups);
            }
        });
    }

    private String getGroupMembers(String[] members) {
        String memberStr = "";
        if (members.length > 0) {
            for (String s : members) {
                memberStr += s + ",";
            }
        }
        Log.e(TAG,"memberStr=" + memberStr);
        return memberStr;
    }


    public void back(View view) {
        finish();
    }

    private void createGroupSuccess() {
        Log.e(TAG, "createGroupSuccess");
        if (progressDialog != null && progressDialog.isShowing()) {
            runOnUiThread(new Runnable() {
                public void run() {
                    progressDialog.dismiss();
                    setResult(RESULT_OK);
                    finish();
                }
            });
        }
    }

    @OnClick(R.id.layout_group_icon)
    public void onClick() {
        uploadHeadPhoto();
    }

    private void uploadHeadPhoto() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dl_title_upload_photo);
        builder.setItems(new String[]{getString(R.string.dl_msg_take_photo), getString(R.string.dl_msg_local_upload)},
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            case 0:
                                Toast.makeText(NewGroupActivity.this, getString(R.string.toast_no_support),
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
                                pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                                startActivityForResult(pickIntent, I.REQUESTCODE_PICK);
                                break;
                            default:
                                break;
                        }
                    }
                });
        builder.create().show();
    }


    public void startPhotoZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("return-data", true);
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, I.REQUESTCODE_CUTTING);
    }

    @OnClick(R.id.btn_save)
    public void save() {
        String name = editGroupName.getText().toString();
        if (TextUtils.isEmpty(name)) {
            new EaseAlertDialog(this, R.string.Group_name_cannot_be_empty).show();
        } else {
            // select from contact list
            startActivityForResult(new Intent(this, GroupPickContactsActivity.class).putExtra("groupName", name), I.REQUESTCODE_MEMBER);
        }
    }
}
