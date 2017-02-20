package cn.ucai.superwechat.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseUserUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.I;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.SuperWeChatHelper;
import cn.ucai.superwechat.domain.Result;
import cn.ucai.superwechat.net.NetDao;
import cn.ucai.superwechat.net.OnCompleteListener;
import cn.ucai.superwechat.utils.MFGT;
import cn.ucai.superwechat.utils.ResultUtils;

public class FriendProfileActivity extends BaseActivity {
    private static final String TAG = FriendProfileActivity.class.getSimpleName();

    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.txt_title)
    TextView txtTitle;
    @BindView(R.id.profile_image)
    ImageView profileImage;
    @BindView(R.id.tv_userinfo_nick)
    TextView tvUserinfoNick;
    @BindView(R.id.tv_userinfo_name)
    TextView tvUserinfoName;
    @BindView(R.id.btn_add_contact)
    Button btnAddContact;
    @BindView(R.id.btn_send_msg)
    Button btnSendMsg;
    @BindView(R.id.btn_send_video)
    Button btnSendVideo;

    User user = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);
        ButterKnife.bind(this);
        initData();
    }

    private void initData() {
        imgBack.setVisibility(View.VISIBLE);
        txtTitle.setVisibility(View.VISIBLE);
        txtTitle.setText(R.string.userinfo_txt_profile);
        user = (User) getIntent().getSerializableExtra(I.User.USER_NAME);
        Log.e(TAG, "user=" + user);
        if (user != null) {
            showUserInfo();
        } else {
            String username = getIntent().getStringExtra("application");
            if (username == null) {
                MFGT.finish(this);
            } else {
                //根据用户名查找个人信息
                syncUserInfo(username);
            }
        }
    }

    private void syncUserInfo(String username) {
        NetDao.findUserByUserName(this, username, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (s != null) {
                    Result result = ResultUtils.getResultFromJson(s, User.class);
                    if (result != null && result.isRetMsg()) {
                        User u = (User) result.getRetData();
                        if (u != null) {
                            user = u;
                            showUserInfo();
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    private void showUserInfo() {
        tvUserinfoNick.setText(user.getMUserNick());
        EaseUserUtils.setAppUserAvatarByPath(this, user.getAvatar(), profileImage,null);
        tvUserinfoName.setText("微信号:" + user.getMUserName());
        if (isFriend()) {
            btnSendMsg.setVisibility(View.VISIBLE);
            btnSendVideo.setVisibility(View.VISIBLE);
        } else {
            btnAddContact.setVisibility(View.VISIBLE);
        }
    }

    private boolean isFriend() {
        User u = SuperWeChatHelper.getInstance().getAppContactList().get(this.user.getMUserName());
        if (u == null) {
            return false;
        } else {
            //是你的好友
            SuperWeChatHelper.getInstance().saveAppContact(user);
            return true;
        }
    }

    @OnClick(R.id.img_back)
    public void onClick() {
        finish();
    }

    @OnClick(R.id.btn_add_contact)
    public void sendAddContactMsg() {
        MFGT.gotoAddContact(this, user.getMUserName());
    }

    @OnClick(R.id.btn_send_msg)
    public void sendMsg(){
        MFGT.gotoChat(this,user.getMUserName());
    }
}
