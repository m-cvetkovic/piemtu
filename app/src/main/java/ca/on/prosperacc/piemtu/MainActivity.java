package ca.on.prosperacc.piemtu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import eu.chainfire.libsuperuser.Shell;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

// Testing with adb:
//
// Read:
//   adb shell "cat /proc/sys/net/ipv4/ip_no_pmtu_disc"
//   adb shell "sysctl -n net.ipv4.ip_no_pmtu_disc"
//
// Write:
//   adb shell "su -c 'echo 0 > /proc/sys/net/ipv4/ip_no_pmtu_disc'"
//   adb shell "su -c 'sysctl -w net.ipv4.ip_no_pmtu_disc 0'"
//
public class MainActivity extends AppCompatActivity
{
    private static final String PATH = "/proc/sys/net/ipv4/ip_no_pmtu_disc";

    private final static File _procFile = new File (PATH);

    // Model classes must be public
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public static class VM extends ViewModel
    {
        final MutableLiveData<Boolean> _no_pmtu = new MutableLiveData<> ();
        final MutableLiveData<Boolean> _has_su = new MutableLiveData<> ();
	final MutableLiveData<String> _error = new MutableLiveData<> ();

	private static Shell.Interactive _rootSession;

	// We don't need to be root to read the value of
	// /proc/sys/net/ipv4/ip_no_pmtu_disc
	//
	// This is less expensive than "sysctl net.ipv4.ip_no_pmtu_disc"
	//
	private boolean getValue ()
	    throws IOException
	{
	    try (FileInputStream in = new FileInputStream (_procFile))
	    {
		int value = in.read();
		return value != '0';
	    }
	}

	void init ()
	{
	    try
	    {
		_no_pmtu.setValue (getValue ());
	    }
	    catch (IOException e)
	    {
	        _error.setValue (e.getMessage ());
	    }
	    _rootSession = new Shell.Builder ()
		.useSU ()
		.setWatchdogTimeout (5)
		.open ((success, reason) -> _has_su.setValue (success));
	}

	void writeToProcFS (boolean value)
	{
	    String cmd = "sysctl -w net.ipv4.ip_no_pmtu_disc=" + (value ? 1 : 0);
	    _rootSession.addCommand (
		cmd,
		0,
		(Shell.OnCommandResultListener2)
		    (commandCode, exitCode, STDOUT, STDERR) ->
		    {
			if (exitCode == 0)
			{
			    try
			    {
				_no_pmtu.setValue (getValue ());
			    }
			    catch (IOException e)
			    {
				_error.setValue (e.getMessage ());
			    }
			}
			else
			{
			    String msg = "Error: " + exitCode + STDERR;
			    _error.setValue (msg);
			}
		    });
	}
    }

    private VM _vm;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
	super.onCreate (savedInstanceState);
	setContentView (R.layout.activity_main);

	TextView tvVersion = findViewById (R.id.version_tv);
	tvVersion.setText (
	    getString (R.string.version, BuildConfig.VERSION_NAME));

	_vm = new ViewModelProvider(this).get (VM.class);
	_vm.init ();
	_vm._no_pmtu.observe (
	    this,
	    no_pmtu ->
	    {
		TextView tv = findViewById (R.id.pmtu_tv);
	        tv.setText (no_pmtu == null ? "." : no_pmtu ? "1" : "0");
		updateButtons (_vm._has_su.getValue (), no_pmtu);
	    });
	_vm._has_su.observe (
	    this,
	    has_su ->
	    {
	        View view = findViewById (R.id.need_su_label);
	        if (has_su == null || !has_su)
		{
		    view.setVisibility (View.VISIBLE);
		}
	        else
		{
		    view.setVisibility (View.INVISIBLE);
		}
		updateButtons (has_su, _vm._no_pmtu.getValue ());
	    });
	_vm._error.observe (
	    this,
	    error ->
	    {
	        TextView tv = findViewById (R.id.error_tv);
	        tv.setText (error != null ? error : "");
	    });
    }

    private void updateButtons (Boolean has_su, Boolean no_pmtu)
    {
	boolean enable = has_su != null && has_su && no_pmtu != null;
	findViewById (R.id.button_clear).setEnabled (enable && no_pmtu);
	findViewById (R.id.button_set).setEnabled (enable && !no_pmtu);
    }

    public void onSetClicked (@SuppressWarnings("unused") View view)
    {
	_vm.writeToProcFS (true);
    }

    public void onClearClicked (@SuppressWarnings("unused") View view)
    {
	_vm.writeToProcFS (false);
    }
}
