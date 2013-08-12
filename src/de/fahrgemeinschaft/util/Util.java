package de.fahrgemeinschaft.util;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.teleportr.ConnectorService;
import org.teleportr.Place;
import org.teleportr.Ride;
import org.teleportr.Ride.COLUMNS;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;
import de.fahrgemeinschaft.R;

public class Util {

    public static boolean handleRideAction(int i, Ride r, FragmentActivity c) {
        switch (i) {
        case R.id.toggle_active:
            if (r.isActive()) {
                r.deactivate().dirty().store(c);
            } else {
                r.activate().dirty().store(c);
            }
            c.startService(new Intent(c, ConnectorService.class)
                    .setAction(ConnectorService.PUBLISH));
            return true;
        case R.id.delete:
            r.delete(c);
            c.startService(new Intent(c, ConnectorService.class)
                    .setAction(ConnectorService.PUBLISH));
            c.getSupportFragmentManager().popBackStack();
            return true;
        case R.id.edit:
            c.startActivity(new Intent(Intent.ACTION_EDIT, Uri.parse(
                    "content://de.fahrgemeinschaft/rides/" + r.getId())));
            return true;
        case R.id.duplicate:
            r.ref("");
            c.startActivity(new Intent(Intent.ACTION_EDIT, r.store(c)));
            return true;
        case R.id.duplicate_retour:
            r.ref("");
            List<Place> vias = r.getVias();
            Place from = r.getFrom();
            r.removeVias();
            r.from(r.getTo());
            for (int j = vias.size() - 1; j >= 0; j--) {
                r.via(vias.get(j));
            }
            r.to(from);
            c.startActivity(new Intent(Intent.ACTION_EDIT, r.store(c)));
            return true;
        case R.id.show_website:
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "http://www.fahrgemeinschaft.de/" +
                    "tripdetails.php?trip=" + r.getRef()
                    )).setClass(c, WebActivity.class));
            return true;
        case R.id.share:
            Intent share = new Intent(android.content.Intent.ACTION_SEND);
            share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT,
                    "From " + r.getFrom().getName()
                    + " to " + r.getTo().getName());
            share.putExtra(Intent.EXTRA_TEXT, 
                    "http://www.fahrgemeinschaft.de/" +
                    "tripdetails.php?trip=" + r.getRef());
            c.startActivity(Intent.createChooser(share,
                    c.getString(R.string.share)));
            return true;
        }
        return false;
    }



    public static boolean isVisible(String key, JSONObject details) {
        try {
            return details.getJSONObject("privacy").getInt(key) == 1;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static final String EMAIL = "EMail";
    private static final String MOBILE = "Mobile";
    private static final String LANDLINE = "Landline";

    public static void openContactOptionsChooserDialog(Context ctx, Cursor c) {
        String route = c.getString(COLUMNS.FROM_NAME)
                + " -> " + c.getString(COLUMNS.TO_NAME);
        Intent contact = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        contact.putExtra(Insert.NAME, route);
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(
                "http://www.fahrgemeinschaft.de/" +
                "tripdetails.php?trip=" + c.getString(COLUMNS.REF)))
                        .setClass(ctx, WebActivity.class);
        ArrayList<Intent> intents = new ArrayList<Intent>();
        JSONObject details = Ride.getDetails(c);
        String dingens;
        try {
            JSONObject privacy = details.getJSONObject("privacy");
            dingens = details.getString(MOBILE);
            if (dingens != null && privacy.getInt(MOBILE) == 1) {
                contact.putExtra(Insert.PHONE, dingens);
                Intent call = labeledIntent(callIntent(dingens),
                        R.drawable.ic_call, dingens, ctx);
                if (call != null) intents.add(call);
                Intent sms = labeledIntent(smsIntent(dingens, route),
                        R.drawable.ic_sms, dingens, ctx);
                if (sms != null) intents.add(sms);
            }
            dingens = details.getString(LANDLINE);
            if (dingens != null && privacy.getInt(LANDLINE) == 1) {
                dingens = dingens.substring(1);
                contact.putExtra(Insert.SECONDARY_PHONE, dingens);
                Intent call = labeledIntent(callIntent(dingens),
                        R.drawable.ic_dial, dingens, ctx);
                if (call != null) intents.add(call);
            }
            dingens = details.getString(EMAIL);
            if (dingens != null && privacy.getInt(EMAIL) == 1) {
                dingens = dingens.substring(1);
                contact.putExtra(Insert.EMAIL, dingens);
                Intent mail = labeledIntent(mailIntent(dingens, route),
                        R.drawable.ic_mail, dingens, ctx);
                if (mail != null) intents.add(mail);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (intents.size() == 0) {
            Toast.makeText(ctx, "private", Toast.LENGTH_SHORT).show();
        } else {
            if (PreferenceManager.getDefaultSharedPreferences(ctx)
                    .getBoolean("create_contact", false)) intents.add(contact);
            ctx.startActivity(Intent.createChooser(web,
                    ctx.getString(R.string.contact))
                        .putExtra(Intent.EXTRA_INITIAL_INTENTS,
                            intents.toArray(new Parcelable[intents.size()])));
        }
    }

    public static LabeledIntent labeledIntent(Intent intent,
                int icon, String label, Context ctx) {
        System.out.println("resolving " + label);
        PackageManager pm = ctx.getPackageManager();
        ComponentName cmp = intent.resolveActivity(pm);
        if (cmp != null) {
            System.out.println("found " + cmp);
            intent.setComponent(cmp);
            Intent resolved = new Intent();
            resolved.setData(intent.getData());
            resolved.setComponent(cmp);
            resolved.setAction(intent.getAction());
            if (intent.getExtras() != null)
                resolved.putExtras(intent.getExtras());
            return new LabeledIntent(intent, ctx.getPackageName(), label, icon);
        }
        return null;
    }

    private static Intent callIntent(String num) {
        Intent call = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + num));
        return call;
    }

    private static Intent smsIntent(String num, String text) {
        Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:" + num));
        sms.putExtra("sms_body", text + "\n noch Platz?");
        sms.addCategory(Intent.CATEGORY_DEFAULT);
//        sms.setType("vnd.android-dir/mms-sms");
        return sms;
    }

    private static Intent mailIntent(String a, String text) {
        Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"+ a));
        mail.putExtra(Intent.EXTRA_TEXT, text + "\n noch Platz frei?");
        mail.putExtra(Intent.EXTRA_SUBJECT, "Fahrgemeinschaft");
//        mail.setType("plain/text");
        return mail;
    }

    public static void fixBackgroundRepeat(View view) {
        Drawable bg = view.getBackground();
        if (bg != null) {
            if (bg instanceof BitmapDrawable) {
                BitmapDrawable bmp = (BitmapDrawable) bg;
                bmp.mutate(); // make sure that we aren't sharing state anymore
                bmp.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            }
        }
    }
}
