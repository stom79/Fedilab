package app.fedilab.android.peertube.drawer;
/* Copyright 2023 Thomas Schneider
 *
 * This file is a part of Fedilab
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Fedilab is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Fedilab; if not,
 * see <http://www.gnu.org/licenses>. */


import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.MUTE;
import static app.fedilab.android.peertube.client.RetrofitPeertubeAPI.ActionType.REPLY;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.fedilab.android.R;
import app.fedilab.android.databinding.DrawerCommentPeertubeBinding;
import app.fedilab.android.peertube.activities.PeertubeActivity;
import app.fedilab.android.peertube.activities.ShowAccountActivity;
import app.fedilab.android.peertube.client.APIResponse;
import app.fedilab.android.peertube.client.RetrofitPeertubeAPI;
import app.fedilab.android.peertube.client.data.CommentData.Comment;
import app.fedilab.android.peertube.client.entities.Report;
import app.fedilab.android.peertube.helper.CommentDecorationHelper;
import app.fedilab.android.peertube.helper.EmojiHelper;
import app.fedilab.android.peertube.helper.Helper;
import app.fedilab.android.peertube.viewmodel.PostActionsVM;
import es.dmoral.toasty.Toasty;


public class CommentListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private final List<Comment> comments;
    private final CommentListAdapter commentListAdapter;
    private final boolean isThread;
    private final String instance;
    private final boolean sepiaSearch;
    public AllCommentRemoved allCommentRemoved;
    boolean isVideoOwner;
    private Context context;

    public CommentListAdapter(List<Comment> comments, boolean isVideoOwner, boolean isThread, String instance, boolean sepiaSearch) {
        this.comments = comments;
        commentListAdapter = this;
        this.isVideoOwner = isVideoOwner;
        this.isThread = isThread;
        this.instance = instance;
        this.sepiaSearch = sepiaSearch;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        context = parent.getContext();
        DrawerCommentPeertubeBinding itemBinding = DrawerCommentPeertubeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }


    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int i) {

        context = viewHolder.itemView.getContext();

        final ViewHolder holder = (ViewHolder) viewHolder;

        final Comment comment = comments.get(i);


        if (comment == null)
            return;
        holder.binding.mainContainer.setTag(i);


        holder.binding.decorationContainer.removeAllViews();
        if (comment.isReply()) {
            int ident = CommentDecorationHelper.getIndentation(comment.getInReplyToCommentId(), comments);
            for (int j = 0; j <= ident; j++) {
                View view = new View(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT);
                params.setMargins((int) Helper.convertDpToPixel(5, context), 0, 0, 0);
                view.setBackgroundResource(R.color.colorAccent);
                view.setLayoutParams(params);
                holder.binding.decorationContainer.addView(view, 0);
            }
        }
        holder.binding.moreActions.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(context, holder.binding.moreActions);
            popup.getMenuInflater()
                    .inflate(R.menu.comment_menu_peertube, popup.getMenu());
            if (!Helper.isOwner(context, comment.getAccount())) {
                popup.getMenu().findItem(R.id.action_delete).setVisible(false);
            } else {
                popup.getMenu().findItem(R.id.action_mute).setVisible(false);
                popup.getMenu().findItem(R.id.action_remove_comments).setVisible(false);
                popup.getMenu().findItem(R.id.action_report).setVisible(false);
            }
            if (!isVideoOwner) {
                popup.getMenu().findItem(R.id.action_remove_comments).setVisible(false);
            }
            if (!Helper.isLoggedIn()) {
                popup.getMenu().findItem(R.id.action_mute).setVisible(false);
                popup.getMenu().findItem(R.id.action_remove_comments).setVisible(false);
                popup.getMenu().findItem(R.id.action_delete).setVisible(false);
            }
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                MaterialAlertDialogBuilder builder;
                if (itemId == R.id.action_delete) {
                    builder = new MaterialAlertDialogBuilder(context);
                    builder.setTitle(R.string.delete_comment);
                    builder.setMessage(R.string.delete_comment_confirm);
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.delete, (dialog, which) -> {
                                new Thread(() -> {
                                    new RetrofitPeertubeAPI(context).post(RetrofitPeertubeAPI.ActionType.PEERTUBEDELETECOMMENT, comment.getVideoId(), comment.getId());
                                    Handler mainHandler = new Handler(Looper.getMainLooper());
                                    Runnable myRunnable = () -> {
                                        comments.remove(comment);
                                        notifyDataSetChanged();
                                        if (comments.size() == 0) {
                                            allCommentRemoved.onAllCommentRemoved();
                                        }
                                    };
                                    mainHandler.post(myRunnable);
                                }).start();

                                dialog.dismiss();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                            .show();
                } else if (itemId == R.id.action_report) {
                    reportComment(comment);
                } else if (itemId == R.id.action_mute) {
                    PostActionsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PostActionsVM.class);
                    viewModel.post(MUTE, comment.getAccount().getAcct(), null).observe((LifecycleOwner) context, apiResponse -> manageVIewPostActions(MUTE, 0, apiResponse));
                    comments.remove(comment);
                    notifyDataSetChanged();
                    if (comments.size() == 0) {
                        allCommentRemoved.onAllCommentRemoved();
                    }
                } else if (itemId == R.id.action_remove_comments) {
                    builder = new MaterialAlertDialogBuilder(context);
                    builder.setTitle(R.string.delete_account_comment);
                    builder.setMessage(R.string.delete_account_comment_confirm);
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.delete, (dialog, which) -> {
                                new Thread(() -> {
                                    new RetrofitPeertubeAPI(context).post(RetrofitPeertubeAPI.ActionType.PEERTUBE_DELETE_ALL_COMMENT_FOR_ACCOUNT, comment.getAccount().getAcct(), "my-videos");
                                    Handler mainHandler = new Handler(Looper.getMainLooper());
                                    Runnable myRunnable = () -> {
                                        comments.remove(comment);
                                        notifyDataSetChanged();
                                        if (comments.size() == 0) {
                                            allCommentRemoved.onAllCommentRemoved();
                                        }
                                    };
                                    mainHandler.post(myRunnable);
                                }).start();

                                dialog.dismiss();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                            .show();
                }
                return true;
            });
            popup.show();
        });
        holder.binding.commentContent.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP && !view.hasFocus()) {
                try {
                    view.requestFocus();
                } catch (Exception ignored) {
                }
            }
            return false;
        });

        Spanned commentSpan;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            commentSpan = Html.fromHtml(EmojiHelper.shortnameToUnicode(comment.getText()), Html.FROM_HTML_MODE_COMPACT);
        else
            commentSpan = Html.fromHtml(EmojiHelper.shortnameToUnicode(comment.getText()));
        holder.binding.commentContent.setText(commentSpan, TextView.BufferType.SPANNABLE);

        holder.binding.commentContent.setMovementMethod(LinkMovementMethod.getInstance());

        holder.binding.commentAccountDisplayname.setText(comment.getAccount().getDisplayName());


        if (context instanceof PeertubeActivity && !isThread) {
            holder.binding.mainContainer.setOnClickListener(v -> ((PeertubeActivity) context).openCommentThread(comment));
            holder.binding.commentContent.setOnClickListener(v -> ((PeertubeActivity) context).openCommentThread(comment));
            holder.binding.replyButton.setOnClickListener(v -> ((PeertubeActivity) context).openCommentThread(comment));
        } else if (context instanceof PeertubeActivity) {
            holder.binding.replyButton.setOnClickListener(v -> ((PeertubeActivity) context).openPostComment(comment, i));
        }
        if (comment.getTotalReplies() > 0) {
            holder.binding.numberOfReplies.setVisibility(View.VISIBLE);
            holder.binding.numberOfReplies.setText(context.getResources().getQuantityString(R.plurals.number_of_replies, comment.getTotalReplies(), comment.getTotalReplies()));
        } else {
            holder.binding.numberOfReplies.setVisibility(View.GONE);
        }

        if (comment.getAccount() != null) {
            Spannable wordtoSpan;
            Pattern hashAcct;
            wordtoSpan = new SpannableString("@" + comment.getAccount().getAcct());
            hashAcct = Pattern.compile("(@" + comment.getAccount().getAcct() + ")");
            Matcher matcherAcct = hashAcct.matcher(wordtoSpan);
            while (matcherAcct.find()) {
                int matchStart = matcherAcct.start(1);
                int matchEnd = matcherAcct.end();
                if (wordtoSpan.length() >= matchEnd && matchStart < matchEnd) {
                    wordtoSpan.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, android.R.color.darker_gray)), matchStart, matchEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
            holder.binding.commentAccountUsername.setText(wordtoSpan);
        }

        holder.binding.commentDate.setText(Helper.dateDiff(context, comment.getCreatedAt()));

        Helper.loadAvatar(context, comment.getAccount(), holder.binding.commentAccountProfile);
        holder.binding.commentAccountProfile.setOnClickListener(v -> {
            Bundle b = new Bundle();
            Intent intent = new Intent(context, ShowAccountActivity.class);
            b.putSerializable("account", comment.getAccount());
            b.putString("accountAcct", comment.getAccount().getAcct());
            intent.putExtras(b);
            context.startActivity(intent);
        });
        if (comment.isReply()) {
            holder.binding.replyButton.setVisibility(View.VISIBLE);
        } else {
            holder.binding.replyButton.setVisibility(View.GONE);
        }
        if (i == 0 && isThread) {
            holder.binding.postReplyButton.setVisibility(View.VISIBLE);
        } else {
            holder.binding.postReplyButton.setVisibility(View.GONE);
        }
        holder.binding.postReplyButton.setOnClickListener(v -> {
            if (Helper.isLoggedIn() && !sepiaSearch) {
                ((PeertubeActivity) context).openPostComment(comment, i);
            } else {
                if (sepiaSearch) {
                    Toasty.info(context, context.getString(R.string.federation_issue), Toasty.LENGTH_SHORT).show();
                } else {
                    Toasty.error(context, context.getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
                }
            }

        });
        if (Helper.isLoggedIn() && !sepiaSearch) {
            holder.binding.replyButton.setVisibility(View.VISIBLE);
        } else {
            holder.binding.replyButton.setVisibility(View.GONE);
        }

    }

    public void manageVIewPostActions(RetrofitPeertubeAPI.ActionType statusAction, int i, APIResponse apiResponse) {

        if (apiResponse.getError() != null) {
            Toasty.error(context, apiResponse.getError().getError(), Toast.LENGTH_LONG).show();
            return;
        }
        if (statusAction == RetrofitPeertubeAPI.ActionType.PEERTUBEDELETECOMMENT) {
            int position = 0;
            for (Comment comment : comments) {
                if (comment.getId().equals(apiResponse.getTargetedId())) {
                    comments.remove(comment);
                    commentListAdapter.notifyItemRemoved(position);
                    break;
                }
                position++;
            }
        } else if (statusAction == REPLY) {
            if (apiResponse.getComments() != null && apiResponse.getComments().size() > 0) {
                comments.add(i + 1, apiResponse.getComments().get(0));
                notifyItemInserted(i + 1);
            }
        } else if (statusAction == RetrofitPeertubeAPI.ActionType.REPORT_COMMENT) {
            Toasty.success(context, context.getString(R.string.successful_report_comment), Toasty.LENGTH_LONG).show();
        } else if (statusAction == MUTE) {
            Toasty.info(context, context.getString(R.string.muted_done), Toast.LENGTH_LONG).show();
        }
    }


    private void reportComment(Comment comment) {
        androidx.appcompat.app.AlertDialog.Builder dialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(context);
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.popup_report_peertube, new LinearLayout(context), false);
        dialogBuilder.setView(dialogView);
        EditText report_content = dialogView.findViewById(R.id.report_content);
        dialogBuilder.setNeutralButton(R.string.cancel, (dialog, id) -> dialog.dismiss());
        dialogBuilder.setPositiveButton(R.string.report, (dialog, id) -> {
            if (report_content.getText().toString().trim().length() == 0) {
                Toasty.info(context, context.getString(R.string.report_comment_size), Toasty.LENGTH_LONG).show();
            } else {
                PostActionsVM viewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(PostActionsVM.class);
                Report report = new Report();
                Report.CommentReport commentReport = new Report.CommentReport();
                commentReport.setId(comment.getId());
                report.setComment(commentReport);
                report.setReason(report_content.getText().toString());
                viewModel.report(report).observe((LifecycleOwner) context, apiResponse -> manageVIewPostActions(RetrofitPeertubeAPI.ActionType.REPORT_COMMENT, 0, apiResponse));
                dialog.dismiss();
            }
        });
        androidx.appcompat.app.AlertDialog alertDialog2 = dialogBuilder.create();
        alertDialog2.show();
    }

    public interface AllCommentRemoved {
        void onAllCommentRemoved();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        DrawerCommentPeertubeBinding binding;

        ViewHolder(DrawerCommentPeertubeBinding itemView) {
            super(itemView.getRoot());
            binding = itemView;
        }
    }


}