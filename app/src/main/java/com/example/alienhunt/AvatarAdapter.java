package com.example.alienhunt;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adaptador del carrusel horizontal de avatares.
 * El avatar seleccionado se muestra más grande y con marco; los demás, más pequeños y atenuados.
 */
public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    public interface OnAvatarClickListener {
        void onAvatarClick(int posicion, View vista);
    }

    private final int[] avatares;
    private final OnAvatarClickListener listener;
    private int seleccionado;

    public AvatarAdapter(int[] avatares, int seleccionadoInicial, OnAvatarClickListener listener) {
        this.avatares = avatares;
        this.seleccionado = seleccionadoInicial;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        holder.imgAvatar.setImageResource(avatares[position]);

        if (position == seleccionado) {
            holder.itemView.setScaleX(1.15f);
            holder.itemView.setScaleY(1.15f);
            holder.itemView.setAlpha(1f);
            holder.itemView.setBackground(crearMarco(holder.itemView));
        } else {
            holder.itemView.setScaleX(0.8f);
            holder.itemView.setScaleY(0.8f);
            holder.itemView.setAlpha(0.6f);
            holder.itemView.setBackground(null);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onAvatarClick(pos, v);
            }
        });
    }

    private GradientDrawable crearMarco(View vista) {
        GradientDrawable marco = new GradientDrawable();
        marco.setColor(ContextCompat.getColor(vista.getContext(), R.color.panel));
        marco.setStroke(UiUtils.dp(vista, 3), ContextCompat.getColor(vista.getContext(), R.color.acid));
        marco.setCornerRadius(UiUtils.dp(vista, 16));
        return marco;
    }

    @Override
    public int getItemCount() {
        return avatares.length;
    }

    public void seleccionar(int posicion) {
        if (posicion == seleccionado || posicion < 0 || posicion >= avatares.length) return;
        int anterior = seleccionado;
        seleccionado = posicion;
        notifyItemChanged(anterior);
        notifyItemChanged(seleccionado);
    }

    public int getSeleccionado() {
        return seleccionado;
    }

    public static class AvatarViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgAvatar;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }
    }
}
