/*
 * Juick
 * Copyright (C) 2008-2012, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.juick.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.juick.android.juick.JuickCompatibleURLMessagesSource;
import com.juickadvanced.R;
import de.quist.app.errorreporter.ExceptionReporter;

/**
 *
 * @author Ugnich Anton
 */
public class ExploreActivity extends FragmentActivity implements View.OnClickListener, TagsFragment.TagsFragmentListener {

    private EditText etSearch;
    private int uid = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ExceptionReporter.register(this);
        Utils.updateTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.explore);

        etSearch = (EditText) findViewById(R.id.editSearch);
        (findViewById(R.id.buttonFind)).setOnClickListener(this);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        TagsFragment tf = new TagsFragment();
        Bundle args = new Bundle();
        args.putSerializable("messagesSource", getIntent().getSerializableExtra("messagesSource"));
        uid = getIntent().getIntExtra("uid", -1);
        args.putSerializable("uid", uid);
        tf.setArguments(args);
        ft.add(R.id.tagsfragment, tf);
        ft.commit();
        MainActivity.restyleChildrenOrWidget(getWindow().getDecorView());
    }

    public void onClick(View v) {
        String search = etSearch.getText().toString().trim();
        if (search.length() == 0) {
            Toast.makeText(this, R.string.Enter_a_message, Toast.LENGTH_SHORT).show();
            return;
        }

        if (search.startsWith("*")) {
            Intent i = new Intent(this, MessagesActivity.class);
            String tagg = Uri.encode(search.substring(1));
            JuickCompatibleURLMessagesSource jms = new JuickCompatibleURLMessagesSource(getString(R.string.Tag) + ": " + search.substring(1), this);
            jms.putArg("tag", tagg);
            if (uid > 0) {
                jms.putArg("user_id", "" + uid);
            }
            i.putExtra("messagesSource", jms);
            startActivity(i);

        } else {
            Intent i = new Intent(this, MessagesActivity.class);
            JuickCompatibleURLMessagesSource jms = new JuickCompatibleURLMessagesSource(getString(R.string.Search) + ": " + search, this);
            if (uid > 0) {
                jms.putArg("user_id", "" + uid);
            }
            jms.putArg("search", Uri.encode(search));
            i.putExtra("messagesSource", jms);
            startActivity(i);
        }
    }

    public void onTagClick(String tag, int uid) {
        this.uid = uid;
        etSearch.setText("*" + tag);
    }

    public void onTagLongClick(String tag, int uid) {
        onTagClick(tag, uid);
    }
}
