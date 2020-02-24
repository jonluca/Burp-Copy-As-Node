package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/*
Heavily influenced by https://github.com/PortSwigger/copy-as-python-requests/
Shell code taken from above and modified for node requests
JonLuca De Caro - 10/30/17

I found the decoded data string to be a little buggy, so I removed that functionality. The data string is preserved as a url encoded object
 */
public class BurpExtender implements IBurpExtender, IContextMenuFactory, ClipboardOwner {
    private IExtensionHelpers helpers;

    private final static String NAME = "Copy as Node.js Request";
    private final static String[] NODE_ESCAPE = new String[256];

    // Generate escape sequences for validation checking
    static {
        for (int i = 0x00; i <= 0xFF; i++) {
            NODE_ESCAPE[i] = String.format("\\x%02x", i);
        }
        for (int i = 0x20; i < 0x80; i++) {
            NODE_ESCAPE[i] = String.valueOf((char) i);
        }
        NODE_ESCAPE['\n'] = "\\n";
        NODE_ESCAPE['\r'] = "\\r";
        NODE_ESCAPE['\t'] = "\\t";
        NODE_ESCAPE['"'] = "\\\"";
        NODE_ESCAPE['\\'] = "\\\\";
    }

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        // Internal burp settings and helpers
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName(NAME);
        callbacks.registerContextMenuFactory(this);
    }

    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        final IHttpRequestResponse[] messages = invocation.getSelectedMessages();
        if (messages == null || messages.length == 0) {
            return null;
        }
        JMenuItem i = new JMenuItem(NAME);
        i.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyMessages(messages);
            }
        });
        return Collections.singletonList(i);
    }

    private void copyMessages(IHttpRequestResponse[] messages) {
        StringBuilder node = new StringBuilder("var request = require('request');\n");
        // disable cert checks so things like self-signed certs work
        node.append("process.env['NODE_TLS_REJECT_UNAUTHORIZED'] = 0\n");
        int i = 0;
        // Generate a new request for every regular http request
        for (IHttpRequestResponse message : messages) {
            IRequestInfo ri = helpers.analyzeRequest(message);
            byte[] req = message.getRequest();
            // Generate unique object names, all prepended with burp#_
            String prefix = "burp" + i++ + "_";
            List<String> headers = ri.getHeaders();

            // Create cookie object, if it exists
            boolean cookiesExist = processCookies(prefix, node, headers);
            // Create dataString object, if it exists
            boolean bodyExists = processBody(prefix, node, req, ri);
            // Generate headers
            node.append("var ").append(prefix).append("headers = {\n    ");
            processHeaders(node, headers);
            if (cookiesExist) {
                node.append(",\n    'Cookie': ").append(prefix).append("cookie");
            }
            node.append("\n}");

            // Generate options
            node.append("\n\n").append("var ").append(prefix).append("options = {\n    url: \"");
            node.append(escapeQuotes(ri.getUrl().toString()));
            node.append("\",\n    headers: ").append(prefix).append("headers,\n    method: \"");
            node.append(ri.getMethod().toLowerCase());
            node.append("\",\n");

            // Append body to options object
            if (bodyExists) {
                node.append("    body: ").append(prefix).append("bodyString\n");
            }
            node.append("}");
            //Generate a unique request for each one
            node.append("\nrequest(").append(prefix).append("options, function (error, response, body) {\nconsole.log('statusCode:', response && response.statusCode)\nconsole.log('error: ', error)\nconsole.log('body: ', body)\n})\n\n");
        }

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(node.toString()), this);
    }

    private static boolean processCookies(String prefix, StringBuilder node, List<String> headers) {
        ListIterator<String> iter = headers.listIterator();
        boolean cookiesExist = false;
        while (iter.hasNext()) {
            String header = iter.next();
            if (!header.toLowerCase().startsWith("cookie:")) {
                continue;
            }
            iter.remove();
            cookiesExist = true;
            String cookie = header.substring(8);
            node.append("\n").append("var ").append(prefix).append("cookie = '");
            node.append(escapeQuotes(cookie));
            node.append("'\n");
        }
        return cookiesExist;
    }

    private static void processHeaders(StringBuilder node, List<String> headers) {
        boolean firstHeader = true;
        for (String header : headers) {
            if (header.toLowerCase().startsWith("host:")) {
                continue;
            }
            header = escapeQuotes(header);
            int colonPos = header.indexOf(':');
            if (colonPos == -1) {
                continue;
            }
            if (firstHeader) {
                firstHeader = false;
                node.append("\"");
            } else {
                node.append(", \n    \"");
            }
            node.append(header, 0, colonPos);
            node.append("\": \"");
            node.append(header, colonPos + 2, header.length());
            node.append('"');
        }
    }

    private boolean processBody(String prefix, StringBuilder node, byte[] req, IRequestInfo ri) {
        int bo = ri.getBodyOffset();
        if (bo >= req.length - 2) {
            return false;
        }
        node.append('\n').append("var ").append(prefix).append("bodyString = ");
        escapeBytes(req, node, bo, req.length);
        node.append("\n\n");
        return true;
    }

    private static String escapeQuotes(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private void escapeUrlEncodedBytes(byte[] input, StringBuilder output, int start, int end) {
        if (end > start) {
            byte[] dec = helpers.urlDecode(Arrays.copyOfRange(input, start, end));
            escapeBytes(dec, output, 0, dec.length);
        } else {
            output.append("''");
        }
    }

    private static void escapeBytes(byte[] input, StringBuilder output, int start, int end) {
        output.append('"');
        for (int pos = start; pos < end; pos++) {
            output.append(NODE_ESCAPE[input[pos] & 0xFF]);
        }
        output.append('"');
    }

    @Override
    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
    }
}
